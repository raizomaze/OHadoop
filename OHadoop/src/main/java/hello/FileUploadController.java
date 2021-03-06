package hello;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import groovyjarjarantlr.StringUtils;

@Controller
@RequestMapping("/")
public class FileUploadController<HttpServerResponse, mv> {

	private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

	public static final String WORKING_DIR = "upload-dir";
	public static final String WORKING_DIR_Input = "upload-inputdir";
	

	private final ResourceLoader resourceLoader;

	private final static StringBuilder runtimeOutput = new StringBuilder("Output>>\n");
    private static String runtimeOutputOld = "";
    
    
	@Autowired
	public FileUploadController(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/")
	public String provideUploadInfo(Model model) throws IOException {

		model.addAttribute("files",
				Files.walk(Paths.get(WORKING_DIR)).filter(path -> !path.equals(Paths.get(WORKING_DIR)))
						.map(path -> Paths.get(WORKING_DIR).relativize(path))
						.map(path -> linkTo(methodOn(FileUploadController.class).getFile(path.toString()))
								.withRel(path.toString()))
						.collect(Collectors.toList()));

		return "uploadForm";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/in")
	public String provideUploadInfo1(Model model) throws IOException {

		model.addAttribute("files",
				Files.walk(Paths.get(WORKING_DIR_Input)).filter(path -> !path.equals(Paths.get(WORKING_DIR_Input)))
						.map(path -> Paths.get(WORKING_DIR_Input).relativize(path))
						.map(path -> linkTo(methodOn(FileUploadController.class).getFile(path.toString()))
								.withRel(path.toString()))
						.collect(Collectors.toList()));

		return "uploadForm";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/executeHadopMRJobPage")
	public String provideexecuteHadopMRJobPage(Model model) throws IOException {

		return "uploadForm";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{filename: =+}")
	@ResponseBody
	public ResponseEntity<?> getFile(@PathVariable String filename) {

		try {
			return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(WORKING_DIR, filename).toString()));
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		File workingDir = new File(WORKING_DIR);
		System.out.println("current working directory:" + workingDir.getAbsolutePath());
		String originalFilename = file.getOriginalFilename();
		if (!file.isEmpty()) {
			try {
				Files.copy(file.getInputStream(), Paths.get(WORKING_DIR, originalFilename));
				redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + originalFilename + "!");

				String SFTPHOST = "192.168.43.111";
				int SFTPPORT = 22;
				String SFTPUSER = "cloudera";
				String SFTPPASS = "cloudera";
				String SFTPWORKINGDIR = "/var/tmp";

				Session session = null;
				Channel channel = null;
				ChannelSftp channelSftp = null;

				try {
					JSch jsch = new JSch();
					session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
					session.setPassword(SFTPPASS);
					java.util.Properties config = new java.util.Properties();
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);
					session.connect();
					channel = session.openChannel("sftp");
					
					channel.connect();
					channelSftp = (ChannelSftp) channel;
					// channelSftp.cd(SFTPWORKINGDIR);
					File jarFile = new File(workingDir.getAbsolutePath() + "/" + originalFilename);
					System.out.println("Jar file to be copied:" + jarFile.getAbsolutePath());
					if (jarFile.isFile()) {
						channelSftp.put(new FileInputStream(jarFile), SFTPWORKINGDIR + "/" + originalFilename);
						
					} else {
						System.out.println("Jar file not available!");
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} catch (IOException | RuntimeException e) {
				redirectAttributes.addFlashAttribute("message",
						"Failued to upload " + originalFilename + " => " + e.getMessage());
			}
		} else {
			redirectAttributes.addFlashAttribute("message",
					"Failed to upload " + originalFilename + " because it was empty");
		}

		return "redirect:/";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{filename:: =+}")
	@ResponseBody
	public ResponseEntity<?> getFile1(@PathVariable String filename) {

		try {
			return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(WORKING_DIR_Input, filename).toString()));
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/in")
	public String handleFileUploadInput(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		File workingDir = new File(WORKING_DIR_Input);
		System.out.println("current working directory:" + workingDir.getAbsolutePath());
		String originalFilename = file.getOriginalFilename();
		if (!file.isEmpty()) {
			try {
				Files.copy(file.getInputStream(), Paths.get(WORKING_DIR_Input, originalFilename));
				redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + originalFilename + "!");

				String SFTPHOST = "192.168.43.111";
				int SFTPPORT = 22;
				String SFTPUSER = "cloudera";
				String SFTPPASS = "cloudera";
				String SFTPWORKINGDIR = "/var/tmp";

				Session session = null;
				Channel channel = null;
				ChannelSftp channelSftp = null;
				

				try {
					JSch jsch = new JSch();
					session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
					session.setPassword(SFTPPASS);
					java.util.Properties config = new java.util.Properties();
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);
					session.connect();
					channel = session.openChannel("sftp");
					channel.connect();
					channelSftp = (ChannelSftp) channel;
					// channelSftp.cd(SFTPWORKINGDIR);
					File inputFile = new File(workingDir.getAbsolutePath() + "/" + originalFilename);
					System.out.println("Input file to be copied:" + inputFile.getAbsolutePath());
					if (inputFile.isFile()) {
						channelSftp.put(new FileInputStream(inputFile), SFTPWORKINGDIR + "/" + originalFilename);
						channelSftp.disconnect();
						ChannelExec channelExec= (ChannelExec) session.openChannel("exec");
						String hadoopPutCommand = "hadoop fs -rm /user/cloudera/wordcount/input/"+ originalFilename + ";hadoop fs -put /var/tmp/"+originalFilename+ " /user/cloudera/wordcount/input/"+ originalFilename;
						System.out.println("Command to execute: " + hadoopPutCommand);
						channelExec.setCommand(hadoopPutCommand);
						channelExec.connect();
						int exitStatus = channelExec.getExitStatus();
						System.out.println("Hadoop put command exited with status: " + exitStatus);
                        channelExec.disconnect();
					} else {
						System.out.println("Input file not available!");
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} catch (IOException | RuntimeException e) {
				redirectAttributes.addFlashAttribute("message",
						"Failued to upload or file already exist " + originalFilename + " => " + e.getMessage());
			}
		} else {
			redirectAttributes.addFlashAttribute("message",
					"Failed to upload " + originalFilename + " because it was empty");
		}

		return "redirect:/in";
	}
	
	
	
	@RequestMapping(value = "/executeHadopMRJob", method = RequestMethod.POST)

	public @ResponseBody String executeHadoopMRjob(@RequestBody HadoopMRDetails hadoopdetails) {

		new Thread(new Runnable() {

			@Override
			public void run() {

				// TODO: Construct the hadoop command using the values you get
				// in the
				// POJO HadoopMRDetails
				// Call JSCH to run the hadoop command remotely in the linux
				// machine.
				// I"m expecting that the hadoop jar is already uploaded to the
				// linux
				// machie where hadoop is
				//

				String hadoopjar = hadoopdetails.getHadoopjar();
				String mainclass = hadoopdetails.getMainclass();
				String hadoopMRcommand = "hadoop fs -rmr /user/cloudera/wordcount/output > /dev/null 2>&1;hadoop jar " + hadoopjar + " " + mainclass
						+ " /user/cloudera/wordcount/input /user/cloudera/wordcount/output";
				System.out.println("Hadoop command:" + hadoopMRcommand);
				// return hadoopMRcommand;

				// do business logic
				// return hadoopdetails.toString();
				String SFTPHOST = "192.168.43.111";
				int SFTPPORT = 22;
				String SFTPUSER = "cloudera";
				String SFTPPASS = "cloudera";
				// String SFTPWORKINGDIR = "/home/cloudera/";

				Session session = null;
				// Channel channel = null;
				// ChannelSftp channelSftp = null;

				try {
					JSch jsch = new JSch();
					session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
					session.setPassword(SFTPPASS);
					java.util.Properties config = new java.util.Properties();
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);
					session.connect();
					// String command = "hadoop fs -cat
					// /user/cloudera/wordcount/output/part-r-00000";

					Channel channel = session.openChannel("exec");
					ChannelExec execChannel = (ChannelExec) channel;

					execChannel.setCommand(hadoopMRcommand);
					//
					// channel.setInputStream(null);
					//
					// channel.setOutputStream(System.out);

					// execChannel.setErrStream(System.out);
					InputStream stdoutInputStream = execChannel.getInputStream();
					InputStream stdErrInputStream = execChannel.getErrStream();

					channel.connect();

					byte[] tmp = new byte[1024];
					while (true) {
						while (stdoutInputStream.available() > 0) {
							int i = stdoutInputStream.read(tmp, 0, 1024);
							if (i < 0)
								break;
							// TODO: Capture in a *global* StringBuilder object
							// here
							// Then in a separate controller function return the
							// StringBuilder
							// content every time the function is being called
							String currentchars = new String(tmp, 0, i);
							runtimeOutput.append(currentchars);
							System.out.print(currentchars);
						}

						while (stdErrInputStream.available() > 0) {
							int i = stdErrInputStream.read(tmp, 0, 1024);
							if (i < 0)
								break;
							// TODO: Capture in a *global* StringBuilder object
							// here
							// Then in a separate controller function return the
							// StringBuilder
							// content every time the function is being called
							String currentchars = new String(tmp, 0, i);
							runtimeOutput.append(currentchars);
							System.out.print(currentchars);
							// return currentchars;
						}

						if (channel.isClosed()) {
							if (stdoutInputStream.available() > 0)
								continue;

							String exitStatusString = "exit-status: " + channel.getExitStatus();
							runtimeOutput.append(exitStatusString);
							System.out.println(exitStatusString);
							break;

						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		}).start();

		return "MapReduce job running. Kindly, take the status from a different service. Input from UI:"
				+ hadoopdetails.toString();

	}

	@RequestMapping(method = RequestMethod.GET, value = "/gethadoopMRjoboutput")
	@ResponseBody
	public String gethadoopMRjoboutput() {
		/*
		 * String mRJobOutput = runtimeOutput.toString(); if
		 * (mRJobOutput.isEmpty()) { return "No output"; }
		 */
		//take the difference between runtimeOutput and runtimeOutputOld to get only 
		// the latest updated text
		//String latestTextOnly = getTextDifferece(runtimeOutput.toString(), runtimeOutputOld);
		//take backup of current runtimeOutput
		//runtimeOutputOld = runtimeOutput.toString();
		return runtimeOutput.toString();
	//}

	//private String getTextDifferece(String newText, String oldText) {
		//e.g.: newText = "abcdxyz", oldText="abcd"
		//we want the latest text i.e. newText - oldText
		//return StringUtils.stripFront(newText, oldText);
		
		
	}

}
