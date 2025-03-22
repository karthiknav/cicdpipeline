package io.getarrays.cdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

public class ApplicationStage extends Stage {
    public ApplicationStage(@NotNull Construct scope, @NotNull String id, @Nullable StageProps props ) {
        super(scope, id, props);

        StackProps stackProps = StackProps.builder()
            .env(props.getEnv())
            .build();
        CdkVpcStack cdkVpcStack = new CdkVpcStack(this, "CdkVpcStack", stackProps);
        new CdkEcsStack(this, "EcsStack", null, cdkVpcStack.getVpc());
    }
}
