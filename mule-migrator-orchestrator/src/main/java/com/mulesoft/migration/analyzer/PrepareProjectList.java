
package com.mulesoft.migration.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mulesoft.migration.beans.FileMetaDataBean;
import com.mulesoft.migration.beans.ProjectMetaDataBean;
import com.orchestrator.PropsUtil;

public class PrepareProjectList {

	public static String parentMule3ProjectPath = "/Users/dsuneja/mule3";

	public static String projectName = "/Users/dsuneja/mule3/jobapplicationapi";

	public static Properties prop = PropsUtil.getProps();

	public static void main(String[] args) {
		File currentDir = new File(parentMule3ProjectPath);

//	      Map<String, String> projectMap =  displayDirectoryContents(currentDir);

//	      System.out.println("# of Projects::" +  projectMap.size());

		System.out.println("parentMule3ProjectPath dir---" + projectName);
		File dir = new File(projectName);

		ProjectMetaDataBean projectMetaDataBean = new ProjectMetaDataBean();

		try {
			analyzeProject(dir, projectMetaDataBean);
			System.out.println(projectMetaDataBean.getDwlLinesofCode());

			analyzeSizeOfProject(projectMetaDataBean);

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static ProjectMetaDataBean analyzeProject(String projectName,ProjectMetaDataBean projectMetaDataBean) {

		File currentDir = new File(parentMule3ProjectPath);

//	      Map<String, String> projectMap =  displayDirectoryContents(currentDir);

//	      System.out.println("# of Projects::" +  projectMap.size());
		

		System.out.println("parentMule3ProjectPath dir---" + projectName);
		File dir = new File(projectName);

		//projectMetaDataBean = new ProjectMetaDataBean();

		try {
			analyzeProject(dir, projectMetaDataBean);
			System.out.println(projectMetaDataBean.getDwlLinesofCode());

			analyzeSizeOfProject(projectMetaDataBean);
			projectMetaDataBean
					.setProjectName(Arrays.asList(projectName.split("/")).get(projectName.split("/").length - 1));

			System.out.println(Arrays.asList(projectName.split("/")).get(projectName.split("/").length - 1));
			System.out.println(Arrays.asList(projectName.split("/")));
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return projectMetaDataBean;

	}

	private static void analyzeSizeOfProject(ProjectMetaDataBean projectMetaDataBean) {

		long totalNoOfComponents = projectMetaDataBean.getFileMetaDataMap().values().stream()
				.mapToLong(o -> o.getNumberOfComponents()).sum();

		long totalLinesOfDWLCode = projectMetaDataBean.getFileMetaDataMap().values().stream().mapToLong(o -> o.getDataWeaveCodeLength())
						.sum();

		long numberOfMunits = projectMetaDataBean.getFileMetaDataMap().values().stream()
				.filter(o -> o.getFileType().equalsIgnoreCase("test-suite")).mapToLong(o -> o.getNumberOfTests()).sum();

		double score = totalNoOfComponents * Double.parseDouble(prop.getProperty("componentsWeighFactor"))
				+ totalLinesOfDWLCode * Double.parseDouble(prop.getProperty("dwlLinesOfCodeWeighFactor"))
				+ numberOfMunits * Double.parseDouble(prop.getProperty("munitsWeighFactor"));

		System.out.println("count of components" + totalNoOfComponents);
		System.out.println("count of dwl lines of code " + totalLinesOfDWLCode);
		System.out.println("number of munits" + numberOfMunits);

		projectMetaDataBean.setTotalLinesOfDWLCode(totalLinesOfDWLCode);
		projectMetaDataBean.setTotalNoOfComponents(totalNoOfComponents);
		projectMetaDataBean.setNumberOfMunits(numberOfMunits);

		projectMetaDataBean.setScore(score);

		System.out.println("score is" + score);

	}

	private static Map<String, String> analyzeProject(File dir, ProjectMetaDataBean projectMetaDataBean)
			throws ParserConfigurationException {

		Map<String, String> projectMap = new HashMap<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Map<String, List<String>> componentMap;

		try {
			File[] files = dir.listFiles();
			for (File file : files) {

				String fileName = file.getName();
				String canonicalPath = file.getCanonicalPath();
				String folderSkipList = prop.getProperty("foldersSkipList");

				if (!file.getName().startsWith(".") && !folderSkipList.contains(file.getName())) {

					if (file.isDirectory()) {// ignore all the directories which starts with .
						projectMap.put(file.getName(), file.getCanonicalPath());
//	 	               System.out.println("directory:" + file.getCanonicalPath());
						System.out.println(file.getName());
						analyzeProject(file, projectMetaDataBean);
					} else {

						if (fileName.equalsIgnoreCase("mule-project.xml")) {
							String muleConfigFile = file.getCanonicalPath();
							Map<String, String> map = MuleConfigParser.getMuleProjectMetaData(muleConfigFile, projectMetaDataBean);
							if (map != null) {
								projectMetaDataBean.setMuleVersion(map.get("runtimeId"));

							}
// 	            			System.out.println(map);

						} else if(fileName.equalsIgnoreCase("mule-artifact.json")) {
							Path filepath = Path.of(file.getCanonicalPath());
							String jsonReport = Files.readString(filepath);
							JsonElement element = JsonParser.parseString(jsonReport);
							if (element.isJsonObject()) {
								JsonObject muleArtifact = element.getAsJsonObject();
								projectMetaDataBean.setMuleVersion(muleArtifact.get("minMuleVersion").getAsString());
							}
						}else if (fileName.endsWith(".xml") && (canonicalPath.contains("/src/main/app")
								|| canonicalPath.contains("/src/main/mule"))) {
							System.out.println(fileName + "###### File is a mule configuration file........");

							Map<String, FileMetaDataBean> fileMetaDataMap = projectMetaDataBean.getFileMetaDataMap();

							FileMetaDataBean fileMetaDataBean = fileMetaDataMap.get(fileName);

							if (fileMetaDataBean == null) {
								fileMetaDataBean = new FileMetaDataBean();
							}
							fileMetaDataBean.setTotalNoOfLines(calculateTotalNoOfLines(canonicalPath));

							Document document = builder.parse(new File(canonicalPath));
// 	            		      Document document = builder.parse(new File("/Users/mvijayvargia/Downloads/Mule3/Mule/acsessftpservice/src/main/app/globalconfig.xml"));

							// Normalize the XML Structure; It's just too important !!
							document.getDocumentElement().normalize();

							// Here comes the root node
							Element root = document.getDocumentElement();
// 	            		      System.out.println(root.getNodeName());

							// Get all employees
							NodeList nList = document.getElementsByTagName("mule");

							componentMap = new HashMap<>();

// 	            		      System.out.println(nList);

							ParseUnknownXMLStructure.visitChildNodes(nList, componentMap, fileName, fileMetaDataBean);
							fileMetaDataBean.setNumberOfComponents(new Long(componentMap.keySet().size()));

							System.out.println("components " + componentMap);

							System.out.println("number of components" + fileMetaDataBean.getNumberOfComponents());

							projectMetaDataBean.getFileMetaDataMap().put(fileName, fileMetaDataBean);

						} else if (fileName.endsWith(".dwl") &&

								canonicalPath.contains("/src/main/resources")) {
							DWLAnalyzer.analyzeDwls(canonicalPath, projectMetaDataBean);

						} else if (fileName.endsWith(".xml") && canonicalPath.contains("/src/test/munit")) {
							Document document = builder.parse(new File(canonicalPath));
// 	            		      Document document = builder.parse(new File("/Users/mvijayvargia/Downloads/Mule3/Mule/acsessftpservice/src/main/app/globalconfig.xml"));
							Map<String, FileMetaDataBean> fileMetaDataMap = projectMetaDataBean.getFileMetaDataMap();

							FileMetaDataBean fileMetaDataBean = fileMetaDataMap.get(fileName);

							if (fileMetaDataBean == null) {
								fileMetaDataBean = new FileMetaDataBean();
							}
							fileMetaDataBean.setFileType("test-suite");
							// Normalize the XML Structure; It's just too important !!
							document.getDocumentElement().normalize();

							// Here comes the root node
							Element root = document.getDocumentElement();

							NodeList nList = document.getElementsByTagName("mule");
							System.out.println("=============START ANALYZING THE FILE===============");

							componentMap = new HashMap<>();

							MunitAnalyzer.visitChildNodes(nList, componentMap, "Test", fileMetaDataBean);
							System.out.println(componentMap);

							projectMetaDataBean.getFileMetaDataMap().put(fileName, fileMetaDataBean);
						}

// 	            		System.out.println("     file:" + file.getCanonicalPath() +  " File Name::"+ file.getName());

					}

				} else {
//	        		 System.out.println("File Name starts with:;" + file.getName() + " so ignoring it.");
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return projectMap;
	}

	public static long calculateTotalNoOfLines(String fileName) {

		Path path = Paths.get(fileName);

		long lines = 0;
		try {

			// much slower, this task better with sequence access
			// lines = Files.lines(path).parallel().count();

			lines = Files.lines(path).count();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Total # of lines in the file::" + lines);
		return lines;

	}

	public Map<String, String> listAllProjects(String path) {

		return null;

	}

	public static Map<String, String> displayDirectoryContents(File dir) {

		Map<String, String> projectMap = new HashMap<>();

		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					projectMap.put(file.getName(), file.getCanonicalPath());
					System.out.println("directory:" + file.getCanonicalPath());
//	               System.out.println(file.getName());
//	               displayDirectoryContents(file);
				} else {
//	               System.out.println("     file:" + file.getCanonicalPath() +  " File Name::"+ file.getName());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return projectMap;
	}

}
