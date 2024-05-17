package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.ConnectionType;
import software.amazon.awscdk.services.apigateway.Integration;
import software.amazon.awscdk.services.apigateway.IntegrationOptions;
import software.amazon.awscdk.services.apigateway.IntegrationProps;
import software.amazon.awscdk.services.apigateway.IntegrationType;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.constructs.Construct;

public class ApiStack extends Stack {

	public ApiStack(final Construct scope, final String id, final StackProps props, ApiStackProps apiStackProps) {
		
		super(scope, id, props);
		
		RestApi restApi = new RestApi(
				this, 
				"RestApi",
				RestApiProps.builder()
					.restApiName("ECommerceAPI")
					.build()
			);
		
		this.createProductsResource(restApi, apiStackProps);
	}

	private void createProductsResource(RestApi restApi, ApiStackProps apiStackProps) {
		Resource productsResource = restApi.getRoot().addResource("products");
		
		productsResource.addMethod(
				"GET", 
				new Integration(
					IntegrationProps.builder()
						.type(IntegrationType.HTTP_PROXY)
						.integrationHttpMethod("GET")
						.uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products")
						.options(
							IntegrationOptions.builder()
								.vpcLink(apiStackProps.vpcLink())
								.connectionType(ConnectionType.VPC_LINK)
								.build()
						)
						.build()
				)	
			);
		
	}
}

record ApiStackProps(
		NetworkLoadBalancer networkLoadBalancer,
		VpcLink vpcLink
	) {}