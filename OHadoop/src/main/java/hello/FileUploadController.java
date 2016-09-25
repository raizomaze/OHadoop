package hello;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

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
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


@Controller
@RequestMapping("/")
public class FileUploadController<HttpServerResponse, mv> 
{

	private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

	public static final String ROOT = "upload-dir";
    
	private final ResourceLoader resourceLoader;

	private final static StringBuilder runtimeOutput =new StringBuilder("Output>>\n");
	
	@Autowired
	public FileUploadController(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/")
	public String provideUploadInfo(Model model) throws IOException 
	{

		model.addAttribute("files", Files.walk(Paths.get(ROOT))
				.filter(path -> !path.equals(Paths.get(ROOT)))
				.map(path -> Paths.get(ROOT).relativize(path))
				.map(path -> linkTo(methodOn(FileUploadController.class).getFile(path.toString())).withRel(path.toString()))
				.collect(Collectors.toList()));

		return "uploadForm";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/executeHadopMRJobPage")
	public String provideexecuteHadopMRJobPage(Model model) throws IOException 
	{

		model.addAttribute("files", Files.walk(Paths.get(ROOT))
				.filter(path -> !path.equals(Paths.get(ROOT)))
				.map(path -> Paths.get(ROOT).relativize(path))
				.map(path -> linkTo(methodOn(FileUploadController.class).getFile(path.toString())).withRel(path.toString()))
				.collect(Collectors.toList()));

		return "TestTextBoxRead";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{filename:.+}")
	@ResponseBody
	public ResponseEntity<?> getFile(@PathVariable String filename)
	{

		try {
			return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(ROOT, filename).toString()));
		} catch (Exception e) 
		{
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
								   RedirectAttributes redirectAttributes) {

		if (!file.isEmpty())
		{
			try {
				Files.copy(file.getInputStream(), Paths.get(ROOT, file.getOriginalFilename()));
				redirectAttributes.addFlashAttribute("message",
						"You successfully uploaded " + file.getOriginalFilename() + "!");
			} catch (IOException|RuntimeException e) {
				redirectAttributes.addFlashAttribute("message", "Failued to upload " + file.getOriginalFilename() + " => " + e.getMessage());
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Failed to upload " + file.getOriginalFilename() + " because it was empty");
		}

		return "redirect:/";
	}
	
	@RequestMapping(value ="/executeHadopMRJob",method = RequestMethod.POST)
	
	public @ResponseBody String executeHadoopMRjob(@RequestBody HadoopMRDetails hadoopdetails) {
		
		new Thread(new Runnable() {

			@Override
			public void run() {

				// TODO: Construct the hadoop command using the values you get in the
				// POJO HadoopMRDetails
				// Call JSCH to run the hadoop command remotely in the linux machine.
				// I"m expecting that the hadoop jar is already uploaded to the linux
				// machie where hadoop is
				//

				String hadoopjar = hadoopdetails.getHadoopjar();
				String mainclass = hadoopdetails.getMainclass();
				String hadoopMRcommand = "hadoop jar " + hadoopjar + " " + mainclass
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
					//String command = "hadoop fs -cat /user/cloudera/wordcount/output/part-r-00000";

					Channel channel = session.openChannel("exec");
					ChannelExec execChannel = (ChannelExec) channel;

					execChannel.setCommand(hadoopMRcommand);
//
//					channel.setInputStream(null);
//
//					channel.setOutputStream(System.out);

					//execChannel.setErrStream(System.out);
					InputStream stdoutInputStream = execChannel.getInputStream();
					InputStream stdErrInputStream = execChannel.getErrStream();

					channel.connect();

					byte[] tmp = new byte[1024];
					while (true) {
						while (stdoutInputStream.available() > 0) {
							int i = stdoutInputStream.read(tmp, 0, 1024);
							if (i < 0)
								break;
							// TODO: Capture in a *global* StringBuilder object here
							// Then in a separate controller function return the StringBuilder
							// content every time the function is being called
							String currentchars = new String(tmp, 0, i);
							runtimeOutput.append(currentchars);
							System.out.print(currentchars);
						}
						
						while (stdErrInputStream.available() > 0) {
							int i = stdErrInputStream.read(tmp, 0, 1024);
							if (i < 0)
								break;
							// TODO: Capture in a *global* StringBuilder object here
							// Then in a separate controller function return the StringBuilder
							// content every time the function is being called
							String currentchars = new String(tmp, 0, i);
							runtimeOutput.append(currentchars);
							System.out.print(currentchars);
							//return currentchars;
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
		
        return "MapReduce job running. Kindly, take the status from a different service. Input from UI:" + hadoopdetails.toString();
        
	}
	@RequestMapping(method = RequestMethod.GET,value="/gethadoopMRjoboutput")
	@ResponseBody
	public String gethadoopMRjoboutput() {
		/*String mRJobOutput = runtimeOutput.toString();
		if (mRJobOutput.isEmpty()) {
			return "No output";
		}*/
		return runtimeOutput.toString();
	}
	
}
	

