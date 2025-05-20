package io.getarrays.cdk;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;
import java.util.List;

public class CdkVpcStack extends Stack {
    private final Vpc vpc;

    public CdkVpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = Vpc.Builder.create(this, "myVPC")
                .maxAzs(2)
                .natGateways(1)
                .subnetConfiguration(List.of(SubnetConfiguration.builder()
                    .cidrMask(24)
                    .name("PublicSubnet")
                    .subnetType(SubnetType.PUBLIC)
                    .build(),
                SubnetConfiguration.builder()
                    .cidrMask(24)
                    .name("PrivateSubnet")
                    .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                    .build()))
            .build();

        this.vpc.applyRemovalPolicy(RemovalPolicy.DESTROY);
    }

    public Vpc getVpc() {
        return vpc;
    }
}
