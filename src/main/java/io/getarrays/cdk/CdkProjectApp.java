package io.getarrays.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkProjectApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account("206409480438")
                .region("us-east-1")
                .build();

        StackProps props = StackProps.builder()
                .env(env)
                .build();
        CdkVpcStack cdkVpcStack = new CdkVpcStack(app, "CdkVpcStack", props);
        CdkEcsStack ecsStack = new CdkEcsStack(app, "CdkEcsStack", props, cdkVpcStack.getVpc());
        new CdkCodePipelineStack(app, "CdkCodePipelineStack", props, ecsStack.getService());
        app.synth();
    }
}

