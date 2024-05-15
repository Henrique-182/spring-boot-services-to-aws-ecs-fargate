package com.myorg;

import java.util.Collections;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.apigateway.VpcLinkProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancerProps;
import software.constructs.Construct;

public class NetworkLoadBalancerStack extends Stack {
	
	private final VpcLink vpcLink;
	
	private final NetworkLoadBalancer networkLoadBalancer;
	
	private final ApplicationLoadBalancer applicationLoadBalancer;

	public NetworkLoadBalancerStack(final Construct scope, final String id, final StackProps props, NetworkLoadBalancerStackProps networkLoadBalancerStackProps) {
		
		super(scope, id, props);
		
		this.networkLoadBalancer = new NetworkLoadBalancer(
				this, 
				"NetworkLoadBalancer", 
				NetworkLoadBalancerProps.builder()
					.loadBalancerName("ECommerceNetworkLoadBalancer")
					.internetFacing(false)
					.vpc(networkLoadBalancerStackProps.vpc())
					.build()
			);
		
		this.vpcLink = new VpcLink(
				this, 
				"VpcLink", 
				VpcLinkProps.builder()
					.targets(Collections.singletonList(this.networkLoadBalancer))
					.build()
			);
		
		this.applicationLoadBalancer = new ApplicationLoadBalancer(
				this, 
				"ApplicationLoadBalancer", 
				ApplicationLoadBalancerProps.builder()
					.loadBalancerName("ECommerceApplicationLoadBalancer")
					.internetFacing(false)
					.vpc(networkLoadBalancerStackProps.vpc())
					.build()
			);
	}

	public VpcLink getVpcLink() {
		return vpcLink;
	}

	public NetworkLoadBalancer getNetworkLoadBalancer() {
		return networkLoadBalancer;
	}

	public ApplicationLoadBalancer getApplicationLoadBalancer() {
		return applicationLoadBalancer;
	}
	
}

record NetworkLoadBalancerStackProps(Vpc vpc) {}