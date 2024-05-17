package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class EcommerceEcsCdkApp {
	
    public static void main(final String[] args) {
        App app = new App();
        
        Environment environment = Environment.builder()
        		.account("533267160963")
        		.region("us-east-1")
        		.build();
        
        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "SiecolaCode");
        infraTags.put("cost", "ECommerceInfra");
        		
        EcrStack ecrStack = new EcrStack(
        		app, 
        		"Ecr", 
        		StackProps.builder()
        			.env(environment)
        			.tags(infraTags)
        			.build()
        	);
        
        VpcStack vpcStack = new VpcStack(
        		app, 
        		"Vpc",
        		StackProps.builder()
        			.env(environment)
        			.tags(infraTags)
        			.build()
        	);
        
        ClusterStack clusterStack = new ClusterStack(
        		app, 
        		"Cluster", 
        		StackProps.builder()
    				.env(environment)
    				.tags(infraTags)
    				.build(), 
        		new ClusterStackProps(vpcStack.getVpc())
        	);
        
        clusterStack.addDependency(vpcStack);
        
        NetworkLoadBalancerStack networkLoadBalancerStack = new NetworkLoadBalancerStack(
        		app, 
        		"NetworkLoadBalancer", 
        		StackProps.builder()
					.env(environment)
					.tags(infraTags)
					.build(),
        		new NetworkLoadBalancerStackProps(vpcStack.getVpc())
        	);
        
        networkLoadBalancerStack.addDependency(vpcStack);
        
        Map<String, String> productsServiceTags = new HashMap<>();
        productsServiceTags.put("team", "SiecolaCode");
        productsServiceTags.put("cost", "ProductsService");
        
        ProductsServiceStack productsServiceStack = new ProductsServiceStack(
        		app, 
        		"ProductsService", 
        		StackProps.builder()
        			.env(environment)
        			.tags(productsServiceTags)
        			.build(),
        		new ProductsServiceProps(
        				vpcStack.getVpc(), 
        				clusterStack.getCluster(), 
        				networkLoadBalancerStack.getNetworkLoadBalancer(), 
        				networkLoadBalancerStack.getApplicationLoadBalancer(), 
        				ecrStack.getProductsServiceRepository()
        			)
        	);
        
        productsServiceStack.addDependency(vpcStack);
        productsServiceStack.addDependency(clusterStack);
        productsServiceStack.addDependency(networkLoadBalancerStack);
        productsServiceStack.addDependency(ecrStack);
        
        ApiStack apiStack = new ApiStack(
        		app, 
        		"Api", 
        		StackProps.builder()
    				.env(environment)
    				.tags(infraTags)
    				.build(), 
        		new ApiStackProps(
        				networkLoadBalancerStack.getNetworkLoadBalancer(), 
        				networkLoadBalancerStack.getVpcLink()
        			)
        	);
        
        apiStack.addDependency(networkLoadBalancerStack);

        app.synth();
    }
}

