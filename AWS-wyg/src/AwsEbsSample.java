import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class AwsEbsSample {

	public static void main(String[] args) throws Exception {

		AWSCredentials cre = new PropertiesCredentials(
				AwsEbsSample.class.getResourceAsStream("AwsCredentials.properties"));
		AmazonEC2 ec2 = new AmazonEC2Client(cre);
		ec2.setEndpoint("ec2.us-east-1b.amazonaws.com");
		try {
			DescribeInstancesResult dir = ec2.describeInstances();
			List<Reservation> reservations = dir.getReservations();
			Set<Instance> instances = new HashSet<Instance>();
			String dns = "";
			String ip = "";
			String InstanceId = "";
			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
				if (reservation.getInstances().get(0).getPrivateIpAddress() != null) {
					dns = reservation.getInstances().get(0).getPublicDnsName();
					ip = reservation.getInstances().get(0).getPublicIpAddress();
					InstanceId=reservation.getInstances().get(0).getInstanceId();
					System.out.println("DNS: " + dns);
					System.out.println("IP: " + ip);
					System.out.println("InstanceId: " + InstanceId);
				}
			}
			CreateVolumeRequest cvr=new CreateVolumeRequest(1,"us-east-1b");
			CreateVolumeResult 	cvrslt=ec2.createVolume(cvr);
			while(cvrslt.getVolume().getState()=="creating") Thread.sleep(3000);
			String VolumeId=cvrslt.getVolume().getVolumeId();
			AttachVolumeRequest attachVolumeRequest=new AttachVolumeRequest(VolumeId,InstanceId,"/dev/xvdf");
			AttachVolumeResult attachVolumeResult=ec2.attachVolume(attachVolumeRequest);
			while(attachVolumeResult.getAttachment().getState()=="attaching")Thread.sleep(3000);
			if(attachVolumeResult.getAttachment().getState()=="attached"){
				connectToTheInstance(dns, "ubuntu1404_pv_micro.pem");
				System.out.println("wait 30 seconds");
				Thread.sleep(30000);
			}
			ec2.shutdown();

		} catch (AmazonServiceException ase) {
			ase.printStackTrace();
		}
	}

	public static void connectToTheInstance(String dns, String keyname)
			throws IOException {
		JSch jSch = new JSch();
		try {
			jSch.addIdentity(keyname);

			Session session = jSch.getSession("ec2-user", dns, 22);

			Properties configuration = new Properties();
			configuration.put("StrictHostKeyChecking", "no");
			session.setConfig(configuration);

			session.connect();

			Channel channel = session.openChannel("shell");
			channel.setOutputStream(System.out);

			File shellScript = createShellScript();

			FileInputStream fin = new FileInputStream(shellScript);
			byte fileContent[] = new byte[(int) shellScript.length()];
			fin.read(fileContent);
			InputStream in = new ByteArrayInputStream(fileContent);

			channel.setInputStream(in);

			channel.connect();
		} catch (JSchException e) {
			e.printStackTrace();
		}
	}

	public static File createShellScript() {
		String filename = "YANGGE_commands.sh";
		File scriptFile = new File(filename);
		try {
			PrintStream out = new PrintStream(new FileOutputStream(scriptFile));
			out.println("echo \"Programmatically SSHed into the instance.\"");
			out.println("mkdir /ebs");
			out.println("mkfs -t ext4 /dev/xvdf");
			out.println("mount /dev/xvdf /ebs");
			out.println("echo /dev/xvdf	/ebs	ext4	default	0,0");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scriptFile;
	}
}
