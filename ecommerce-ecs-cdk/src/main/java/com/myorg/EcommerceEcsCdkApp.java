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

        app.synth();
    }
}

