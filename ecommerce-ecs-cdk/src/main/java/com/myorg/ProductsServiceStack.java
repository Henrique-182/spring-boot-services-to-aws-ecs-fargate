package com.myorg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriver;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateServiceProps;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.FargateTaskDefinitionProps;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class ProductsServiceStack extends Stack {

	public ProductsServiceStack(final Construct scope, final String id, final StackProps props, ProductsServiceProps productsServiceProps) {
		
		super(scope, id, props);
		
		Table productsDbd = new Table(
				this, 
				"ProductsDbd", 
				TableProps.builder()
					.partitionKey(
						Attribute.builder()
							.name("id")
							.type(AttributeType.STRING)
							.build()
					)
					.tableName("products")
					.removalPolicy(RemovalPolicy.DESTROY)
					.billingMode(BillingMode.PROVISIONED)
					.readCapacity(1)
					.writeCapacity(1)
					.build()
			);
		
		FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(
				this, 
				"TaskDefinition",
				FargateTaskDefinitionProps.builder()
					.family("products-service")
					.cpu(512)
					.memoryLimitMiB(1024)
					.build()
			);
		
		AwsLogDriver awsLogDriver = new AwsLogDriver(
				AwsLogDriverProps.builder()
					.logGroup(
						new LogGroup(
							this, 
							"LogGroup",
							LogGroupProps.builder()
								.logGroupName("ProductsService")
								.removalPolicy(RemovalPolicy.DESTROY)
								.retention(RetentionDays.ONE_MONTH)
								.build()
						)
					)
					.streamPrefix("ProductsService")
					.build()
			);
		
		Map<String, String> envVariables = new HashMap<>();
		envVariables.put("SERVER_PORT", "8080");
		
		fargateTaskDefinition.addContainer(
				"ProductsServiceContainer", 
				ContainerDefinitionOptions.builder()
					.image(ContainerImage.fromEcrRepository(productsServiceProps.repository(), "1.0.0"))
					.containerName("products-service")
					.logging(awsLogDriver)
					.portMappings(
						Collections.singletonList(
							PortMapping.builder()
							.containerPort(8080)
							.protocol(Protocol.TCP)
								.build()
						)
					)
					.environment(envVariables)
					.build()
			);
		
		ApplicationListener applicationListener = productsServiceProps.applicationLoadBalancer()
				.addListener(
					"ProductsServiceApplicationLoadBalancerListener", 
					ApplicationListenerProps.builder()
						.port(8080)
						.protocol(ApplicationProtocol.HTTP)
						.loadBalancer(productsServiceProps.applicationLoadBalancer())
						.build()
				);
		
		FargateService fargateService = new FargateService(
				this, 
				"ProductsService", 
				FargateServiceProps.builder()
					.serviceName("ProductsService")
					.cluster(productsServiceProps.cluster())
					.taskDefinition(fargateTaskDefinition)
					.desiredCount(2)
					.assignPublicIp(true)
					.build()
			);
		
		productsServiceProps.repository().grantPull(fargateTaskDefinition.getExecutionRole());
		
		fargateService.getConnections().getSecurityGroups().get(0).addIngressRule(Peer.anyIpv4(), Port.tcp(8080));
		
		applicationListener.addTargets(
				"ProductsServiceApplicationLoadBalancerTarget", 
				AddApplicationTargetsProps.builder()
					.targetGroupName("prod-service-app-load-balancer")
					.port(8080)
					.protocol(ApplicationProtocol.HTTP)
					.targets(Collections.singletonList(fargateService))
					.deregistrationDelay(Duration.seconds(30))
					.healthCheck(
						HealthCheck.builder()
							.enabled(true)
							.interval(Duration.seconds(30))
							.timeout(Duration.seconds(10))
							.path("/actuator/health")
							.healthyHttpCodes("200")
							.port("8080")
							.build()
					)
					.build()
			);
		
		NetworkListener networkListener = productsServiceProps.networkLoadBalancer()
				.addListener(
					"ProductsServiceNetworkLoadBalancerListener", 
					BaseNetworkListenerProps.builder()
						.port(8080)
						.protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
						.build()
				);
		
		networkListener.addTargets(
				"ProductsServiceNetworkLoadBalancerTarget", 
				AddNetworkTargetsProps.builder()
					.port(8080)
					.protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
					.targetGroupName("prod-service-net-load-balancer")
					.targets(
						Collections.singletonList(
							fargateService.loadBalancerTarget(
								LoadBalancerTargetOptions.builder()
									.containerName("products-service")
									.protocol(Protocol.TCP)
									.containerPort(8080)
									.build()
							)
						)
					)
					.build()
			);
	}
	
}

record ProductsServiceProps(
	Vpc vpc,
	Cluster cluster,
	NetworkLoadBalancer networkLoadBalancer,
	ApplicationLoadBalancer applicationLoadBalancer,
	Repository repository
) {}
