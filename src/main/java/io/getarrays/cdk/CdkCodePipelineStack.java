package io.getarrays.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ConnectionSourceOptions;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CdkCodePipelineStack extends Stack {
    public CdkCodePipelineStack(final Construct scope, final String id, final StackProps props) {

        super(scope, id, props);

        Artifact sourceArtifact = new Artifact();
        Artifact buildArtifact = new Artifact();

        CodePipelineSource source = CodePipelineSource.connection(
            "karthiknav/cicdpipeline",
            "main",
            ConnectionSourceOptions.builder()
                .connectionArn("arn:aws:codeconnections:us-east-1:206409480438:connection/c12ba6ca-3d36-4ae9-8a2e-51fa31e27866")
                .build()
        );

        /*Pipeline pipeline = Pipeline.Builder.create(this, "CiCDDemoPipeline")
            .restartExecutionOnUpdate(true)
            .build();

        StageOptions sourceStage = StageOptions.builder()
            .stageName("Source")
            .actions(List.of(
                CodeStarConnectionsSourceAction.Builder.create()
                    .actionName("Checkout")
                    .branch("main")
                    .connectionArn("arn:aws:codeconnections:us-east-1:206409480438:connection/c12ba6ca-3d36-4ae9-8a2e-51fa31e27866")
                    .output(sourceArtifact)
                    .owner("karthiknav")
                    .repo("cicdpipeline")
                    .build()
            ))
            .build();

        pipeline.addStage(sourceStage);

        Role codeBuildRole = Role.Builder.create(this, "CodeBuildRole")
            .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
            .managedPolicies(List.of(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryPowerUser")
            ))
            .build();

        PipelineProject project = PipelineProject.Builder.create(this, "Project")
            .role(codeBuildRole)
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_4)
                .privileged(true)
                .environmentVariables(Map.of(
                    "ECR_REGISTRY", BuildEnvironmentVariable.builder().value("206409480438.dkr.ecr.us-east-1.amazonaws.com").build(),
                    "ECR_REPOSITORY", BuildEnvironmentVariable.builder().value("cicdpipeline").build(),
                    "AWS_REGION", BuildEnvironmentVariable.builder().value(this.getRegion()).build()
                ))
                .build())
            .buildSpec(BuildSpec.fromSourceFilename("buildspec.yml"))
            .projectName("cicddemoBuildProject")
            .build();

         CodeBuildAction buildAction = CodeBuildAction.Builder.create()
            .actionName("Build-Action")
            .project(project)
            .input(sourceArtifact)
            .outputs(List.of(buildArtifact))
            .build();

        StageOptions buildStage = StageOptions.builder()
            .stageName("BuildApp")
            .actions(List.of(buildAction))
            .build();



        EcsDeployAction deployAction = EcsDeployAction.Builder.create()
            .actionName("Deploy")
            .service(ecsService)
            .input(buildArtifact)
            .build();

        StageOptions deployStage = StageOptions.builder()
            .stageName("DeployApp")
            .actions(List.of(deployAction))
            .build();*/



       CodePipeline codePipeline = CodePipeline.Builder.create(this,  "synth-pipeline-id")
           .pipelineName("cicddemoPipeline")
            .synth(ShellStep.Builder.create("Synth")
                .input(source)
                .commands(Arrays.asList("npm install -g aws-cdk", "cdk synth"))
                .build())
            .build();

        Role codeBuildRole = Role.Builder.create(this, "CodeBuildRole")
            .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
            .managedPolicies(List.of(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryPowerUser")
            ))
            .build();

        codePipeline.buildPipeline();
        Pipeline pipeline = codePipeline.getPipeline();

        Artifact artifact =
            pipeline.getStages().get(0).getActions().get(0).getActionProperties().getOutputs().get(0);

        PipelineProject project = PipelineProject.Builder.create(this, "Project")
            .role(codeBuildRole)
            .environment(BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.AMAZON_LINUX_2_4)
                .privileged(true)
                .environmentVariables(Map.of(
                    "ECR_REGISTRY", BuildEnvironmentVariable.builder().value("206409480438.dkr.ecr.us-east-1.amazonaws.com").build(),
                    "ECR_REPOSITORY", BuildEnvironmentVariable.builder().value("cicdpipeline").build(),
                    "AWS_REGION", BuildEnvironmentVariable.builder().value(this.getRegion()).build()
                ))
                .build())
            .buildSpec(BuildSpec.fromSourceFilename("buildspec.yml"))
            .projectName("cicddemoBuildProject")
            .build();

        CodeBuildAction buildAction = CodeBuildAction.Builder.create()
            .actionName("Build-Action")
            .project(project)
            .input(artifact)
            .outputs(List.of(buildArtifact))
            .build();
        pipeline.addStage(StageOptions.builder()
            .stageName("BuildApp")
            .actions(List.of(buildAction))
            .build());
        //pipeline.addStage(deployStage);


    }
}
