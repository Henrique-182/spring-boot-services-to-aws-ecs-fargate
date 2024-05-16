package com.myorg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
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
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class ProductsServiceStack extends Stack {

	public ProductsServiceStack(final Construct scope, final String id, final StackProps props, ProductsServiceProps productsServiceProps) {
		
		super(scope, id, props);
		
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
					.build()
			);
		
		productsServiceProps.repository().grantPull(fargateTaskDefinition.getExecutionRole());
		
		fargateService.getConnections().getSecurityGroups().get(0).addIngressRule(Peer.anyIpv4(), Port.tcp(8080));
	}
	
}

record ProductsServiceProps(
	Vpc vpc,
	Cluster cluster,
	NetworkLoadBalancer networkLoadBalancer,
	ApplicationLoadBalancer applicationLoadBalancer,
	Repository repository
) {}
