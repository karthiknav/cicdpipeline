package io.getarrays.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;
import java.util.List;

public class CdkEcsStack extends Stack {
    private final FargateService fargateService;
    public CdkEcsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);
        // create ECS cluster
        Cluster cluster = Cluster.Builder.create(this, "cicdCluster")
                .vpc(vpc)
                .clusterName("cicddemoCluster")
                .build();

        Role ecsTaskExecutionRole = Role.Builder.create(this, "EcsTaskExecutionRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(List.of(
                    ManagedPolicy.fromManagedPolicyArn(this, "AmazonECSTaskExecutionRolePolicy", "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")
                ))
                .build();

        LogGroup logGroup = LogGroup.Builder.create(this, "cicdLogGroup")
                .logGroupName("/ecs/cicddemoLogGroup")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "cicdTaskDefinition")
                .cpu(512)
                .memoryLimitMiB(1024)
                .executionRole(ecsTaskExecutionRole)
                .family("cicdTaskDefinition")
                .build();

        // create ECR repository
        Repository ecrRepository = Repository.Builder.create(this, "cicdRepository")
                .repositoryName("cicdrepository")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        ContainerDefinition containerDefinition = taskDefinition.addContainer("cicdContainer",
                ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromRegistry("206409480438.dkr.ecr.us-east-1.amazonaws.com/cicdpipeline:latest"))
                    .memoryLimitMiB(512)
                    .containerName("cicdcontainer")
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(logGroup)
                        .streamPrefix("ecs")
                        .build()))
                    .build());

        containerDefinition.addPortMappings(PortMapping.builder()
                .containerPort(8080)
                .protocol(software.amazon.awscdk.services.ecs.Protocol.TCP)
            .build());

        // Security Group for ALB
        SecurityGroup albSecurityGroup = SecurityGroup.Builder.create(this, "albSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .securityGroupName("cicddemoAlbSecurityGroup")
                .build();

        albSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "Allow HTTP traffic from anywhere");

        ApplicationLoadBalancer alb = ApplicationLoadBalancer.Builder.create(this, "cicdALB")
                .vpc(vpc)
                .internetFacing(true)
                .loadBalancerName("cicddemoAlb")
                .securityGroup(albSecurityGroup)
                .build();

        ApplicationListener listener = alb.addListener("cicdListener",
            BaseApplicationListenerProps.builder()
                .port(80)
                .open(true)
                .build());

        SecurityGroup ecsSecurityGroup = SecurityGroup.Builder.create(this, "ecsSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();

        ecsSecurityGroup.addIngressRule(albSecurityGroup, Port.tcp(8080), "Allow HTTP traffic from ALB");

        fargateService = FargateService.Builder.create(this, "cicdService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(true)
                .securityGroups(List.of(ecsSecurityGroup))
                .desiredCount(1)
                .serviceName("cicddemoService")
                .vpcSubnets(SubnetSelection.builder()
                    .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                    .build())
                .build();

        listener.addTargets("cicddemoTargetGroup", AddApplicationTargetsProps.builder()
            .port(8080)
            .targets(List.of(fargateService))
                .healthCheck(HealthCheck.builder()
                    .path("/actuator/health")
                    .build())
            .build());

        ScalableTaskCount scalableTaskCount = fargateService.autoScaleTaskCount(EnableScalingProps.builder()
            .minCapacity(2)
            .maxCapacity(4)
            .build());

        scalableTaskCount.scaleOnCpuUtilization("CpuScaling", CpuUtilizationScalingProps.builder()
            .targetUtilizationPercent(50)
            .policyName("cicddemoScalingPolicy")
            .scaleInCooldown(Duration.minutes(1))
            .scaleOutCooldown(Duration.minutes(1))
            .build());
    }

    public FargateService getService() {
        return fargateService;
    }



}
