package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.constructs.Construct;

public class ApiStack extends Stack {

	public ApiStack(final Construct scope, final String id, final StackProps props, ApiStackProps productsServiceProps) {
		
		super(scope, id, props);
		
		RestApi restApi = new RestApi(
				this, 
				"RestApi",
				RestApiProps.builder()
					.restApiName("ECommerceAPI")
					.build()
			);
	}
	
}

record ApiStackProps(
		NetworkLoadBalancer networkLoadBalancer,
		VpcLink vpcLink
	) {}