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
        new CdkCodePipelineStack(app, "CdkCodePipelineStack", props);
        app.synth();
    }
}

