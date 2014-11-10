import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.*;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.LoadBalancer;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CrossZoneLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AwsEbtSample {
	private static AWSCredentials cre;
	private static Region region;
	private static AWSElasticBeanstalk ebtClient;
	private static AmazonS3 s3client;
	private static AmazonElasticLoadBalancingClient elbClient;
	private static String EnvId;

	public static void AwsEbtSampleInit() {
		try {
			cre = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. "
							+ "Please make sure that your credentials file is at the correct "
							+ "location (C:\\Users\\qx\\.aws\\credentials), and is in valid format.",
					e);
		}
		ebtClient = new AWSElasticBeanstalkClient(cre);
		region = Region.getRegion(Regions.AP_NORTHEAST_1);
		ebtClient.setRegion(region);
		s3client = new AmazonS3Client(cre);
		s3client.setRegion(region);
		elbClient = new AmazonElasticLoadBalancingClient(cre);
	}

	public static String CreateS3(String Name, String Description,
			String keyName, String uploadFileName) {
		String bucketLocation = new String();
		try {
			if (!(s3client.doesBucketExist(Name))) {
				// Note that CreateBucketRequest does not specify region. So
				// bucket is
				// created in the region specified in the client.
				s3client.createBucket(new CreateBucketRequest(Name));
				uploadToS3(Name, keyName, uploadFileName);
			}
			// Get location.
			bucketLocation = s3client
					.getBucketLocation(new GetBucketLocationRequest(Name));
			System.out.println("bucket location = " + bucketLocation);
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		return bucketLocation;
	}

	private static void uploadToS3(String bucketName, String keyName,
			String uploadFileName) {
		try {
			System.out.println("Uploading a new object to S3 from a file\n");
			File file = new File(uploadFileName);
			s3client.putObject(new PutObjectRequest(bucketName, keyName, file));

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		return;
	}

	public static void CreateApp(String Name, String Description) {
		DescribeApplicationsRequest describeApplicationsRequest = new DescribeApplicationsRequest();
		DescribeApplicationsResult describeApplicationsResult = ebtClient
				.describeApplications(describeApplicationsRequest);
		for (ApplicationDescription Ads : describeApplicationsResult
				.getApplications()) {
			if (Ads.getApplicationName().contentEquals(Name)) {
				System.out.println("App already exist");
				return;
			}
			System.out.println("Name: " + Ads.getApplicationName() + " Des:"
					+ Ads.getDescription());
		}
		CreateApplicationRequest CreAppRq = new CreateApplicationRequest(Name);
		CreAppRq.setDescription(Description);
		try {
			ebtClient.createApplication(CreAppRq);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		return;
	}

	public static void CreateAppVersion(String AppName, String Version,
			String Description, String S3Name, String S3Key) {
		S3Location sourceBundle = new S3Location().withS3Bucket(S3Name)
				.withS3Key(S3Key);
		CreateApplicationVersionRequest creAppVrReq = new CreateApplicationVersionRequest()
				.withApplicationName(AppName).withSourceBundle(sourceBundle)
				.withDescription(Description).withVersionLabel(Version);
		try {
			ebtClient.createApplicationVersion(creAppVrReq);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		return;
	}

	public static void CheckDNSAvailability(String cNAMEPrefix) {
		CheckDNSAvailabilityRequest availabilityRequest = new CheckDNSAvailabilityRequest()
				.withCNAMEPrefix(cNAMEPrefix);
		CheckDNSAvailabilityResult availabilityResult = ebtClient
				.checkDNSAvailability(availabilityRequest);
		if (availabilityResult.isAvailable()) {
			System.out.println(availabilityResult.getFullyQualifiedCNAME()
					+ " is available. Proceeding.");
		} else {
			System.out.println(String.format("env name '%s' is not available",
					cNAMEPrefix));
			System.exit(1);
		}
	}

	/*
	 * ApplicationName = SampleApp •VersionLabel = Version1 •EnvironmentName
	 * =mynewappenv •CNAMEPrefix = mysampleapplication •Description =
	 * description •OptionSettings.member.1.Namespace =
	 * aws:autoscaling:launchconfiguration •OptionSettings.member.1.OptionName =
	 * IamInstanceProfile •OptionSettings.member.1.Value =
	 * ElasticBeanstalkProfile
	 */
	public static void CreateEnv(String ApplicationName, String VersionLabel,
			String EnvironmentName, String CNAMEPrefix, String Description) {
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
				.withApplicationName(ApplicationName)
				.withVersionLabel(VersionLabel)
				.withEnvironmentNames(EnvironmentName);
		try {
			DescribeEnvironmentsResult rslt = ebtClient
					.describeEnvironments(req);
			if (rslt.getEnvironments().size() > 0) {
				System.out.println(EnvironmentName + ":already exist");
				System.exit(1);
			}
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		CreateConfigurationTemplateRequest cCTr = new CreateConfigurationTemplateRequest(
				ApplicationName, null).withTemplateName(
				EnvironmentName + "template").withSolutionStackName(
				"64bit Amazon Linux running Tomcat 7");
		try {
			ebtClient.createConfigurationTemplate(cCTr);
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		ArrayList<ConfigurationOptionSetting> optionlist = new ArrayList<ConfigurationOptionSetting>();
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBUser", "root"));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBPassword", "rootroot"));
		optionlist.add(new ConfigurationOptionSetting(
				"aws:elasticbeanstalk:application:environment",
				"JDBC_CONNECTION_STRING", ""));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBInstanceClass", "db.t1.micro"));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBAllocatedStorage", "5"));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"MultiAZDatabase", "true"));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBEngineVersion", "5.5"));
		optionlist.add(new ConfigurationOptionSetting("aws:rds:dbinstance",
				"DBEngine", "mysql"));
		CreateEnvironmentRequest request = new CreateEnvironmentRequest(
				ApplicationName, Description).withVersionLabel(VersionLabel)
				.withCNAMEPrefix(CNAMEPrefix)
				.withTemplateName(EnvironmentName + "template")
				.withEnvironmentName(EnvironmentName)
				.withOptionSettings(optionlist);
		try {
			CreateEnvironmentResult reslt = ebtClient
					.createEnvironment(request);
			EnvId = reslt.getEnvironmentId();
		} catch (AmazonServiceException ase) {
			System.out.println(ase.getMessage());
		}
		return;
	}

	public static void DescripeConfigures(String ApplicationName,
			String EnvironmentName) {
		DescribeConfigurationSettingsRequest req0 = new DescribeConfigurationSettingsRequest()
				.withEnvironmentName(EnvironmentName).withApplicationName(
						ApplicationName);
		DescribeConfigurationSettingsResult res0 = ebtClient
				.describeConfigurationSettings(req0);
		for (ConfigurationSettingsDescription csd : res0
				.getConfigurationSettings()) {
			System.out.println(csd.getEnvironmentName() + " settings:");
			for (ConfigurationOptionSetting cos : csd.getOptionSettings()) {
				System.out.println(cos.toString());
			}
		}
		System.out.println("\n\n");
		DescribeConfigurationOptionsRequest req1 = new DescribeConfigurationOptionsRequest()
				.withEnvironmentName(EnvironmentName);
		DescribeConfigurationOptionsResult res1 = ebtClient
				.describeConfigurationOptions(req1);
		for (ConfigurationOptionDescription cod : res1.getOptions()) {
			System.out.println(cod);
		}
	}

	public static void main(String[] args) throws Exception {
		String AppName = new String("testTwitterMapApp2");
		String S3Name = new String("twittermapstorage");
		String S3Key = new String("testTwitterMap.war");
		String Version = new String("002");
		String cNAMEPrefix = new String("testTwitterMapApp002wyg2");
		String EnvironmentName = new String("EnvTwitterMapTest2");
		String SourceFilePath = new String("E:\\TestTwittMap.war");
		AwsEbtSampleInit();
		String S3URL = CreateS3(S3Name, "S3 for TwitterMap", S3Key,
				SourceFilePath);
		System.out.println("S3URL:" + S3URL);
		CreateApp(AppName, "App for TwitterMap");
		CreateAppVersion(AppName, Version, "Second ", S3Name, S3Key);
		CheckDNSAvailability(cNAMEPrefix);
		CreateEnv(AppName, Version, EnvironmentName, cNAMEPrefix,
				"twitterMapApp test env");
		List<LoadBalancer> listLB;
		do {
			Thread.sleep(10000);
			DescribeEnvironmentResourcesRequest desEnvResReq;
			desEnvResReq = new DescribeEnvironmentResourcesRequest()
					.withEnvironmentId(EnvId);
			DescribeEnvironmentResourcesResult desEnvResRes = ebtClient
					.describeEnvironmentResources(desEnvResReq);
			listLB = desEnvResRes.getEnvironmentResources().getLoadBalancers();
		} while (listLB.size() == 0);
		ModifyLoadBalancerAttributesRequest mLBAttReq = new ModifyLoadBalancerAttributesRequest()
				.withLoadBalancerName(listLB.get(0).getName());
		LoadBalancerAttributes LBAttributes = new LoadBalancerAttributes();
		LBAttributes.setCrossZoneLoadBalancing(new CrossZoneLoadBalancing()
				.withEnabled(true));
		mLBAttReq.setLoadBalancerAttributes(LBAttributes);
		elbClient.modifyLoadBalancerAttributes(mLBAttReq);
	}
}