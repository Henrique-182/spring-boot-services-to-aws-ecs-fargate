package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.constructs.Construct;

public class EcrStack extends Stack {
	
	private final Repository productsServiceRepository;

	public EcrStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);
		
		this.productsServiceRepository = new Repository(
				this, 
				"Products-Service", 
				RepositoryProps
					.builder()
					.repositoryName("products-service")
					.removalPolicy(RemovalPolicy.DESTROY)
					.imageTagMutability(TagMutability.IMMUTABLE)
					.emptyOnDelete(true)
					.build()
			);
	}

	public Repository getProductsServiceRepository() {
		return productsServiceRepository;
	}
	
}
