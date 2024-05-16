package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.FargateTaskDefinitionProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
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
	}
	
}

record ProductsServiceProps(
	Vpc vpc,
	Cluster cluster,
	NetworkLoadBalancer networkLoadBalancer,
	ApplicationLoadBalancer applicationLoadBalancer,
	Repository repository
) {}
