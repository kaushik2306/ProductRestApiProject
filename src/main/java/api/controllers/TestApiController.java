package api.controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.testng.ISuite;
import org.testng.ITestNGMethod;
import org.testng.SuiteRunner;
import org.testng.annotations.Test;
import org.testng.internal.Configuration;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.ParallelMode;

import api.services.TestNGService;

@RestController
@RequestMapping("/api/")
public class TestApiController {

	@Autowired
	TestNGService testNgService;

	public TestNGService getTestNgService() {
		return testNgService;
	}

	@RequestMapping(value="initializeTestNG", method=RequestMethod.POST)
	public ResponseEntity<?> initializeTestNG(@RequestBody Map<String, Object> request){
		Map<String, Object> responseMap = new LinkedHashMap<>();
		initMySuiteListener(request, responseMap);
		HttpStatus status = HttpStatus.OK;
		if(responseMap.containsKey("error")){
			status = HttpStatus.FORBIDDEN;
		}
		return new ResponseEntity<>(responseMap, status);
	}



	@RequestMapping(value="testApi", method=RequestMethod.POST)
	public ResponseEntity<?> invokeTestApi(@RequestBody Map<String, Object> request){
		System.out.println("TEST API INVOKED FROM REST");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		String className = (String) request.get("classname");
		String apiName = (String)request.get("apiname");
		List<Object> parameterList = (List<Object>)request.get("parameters");
		Class<?> clazz = null;
		Object clazzInstance = null;
		Method testApiMethod = null;
		try {
			clazz = this.getClazz(className,responseMap);
			if(clazz!=null){				
				clazzInstance = clazz.newInstance();
				testApiMethod = this.getMethod(clazz, apiName, responseMap);
				if(testApiMethod!=null){
					try {						
						testApiMethod.invoke(clazzInstance,parameterList.toArray());
					} catch (Exception e) {
						responseMap.put("error", Arrays.asList(ExceptionUtils.getStackTrace(e)));
					}
				}
			}	
		} catch (Throwable e) {
			String exceptionStackTrace = ExceptionUtils.getStackTrace(e);
			if(responseMap.containsKey("error")){
				List<String> exp = (List<String>)responseMap.get("error");
				exp.add(exceptionStackTrace);
				responseMap.put("error",exp);
			}else{
				responseMap.put("error", Arrays.asList(exceptionStackTrace));
			}
		}
		HttpStatus status = HttpStatus.OK;//Default
		if(responseMap.containsKey("error")){
			status = HttpStatus.FORBIDDEN;//on Error
		}
		return new ResponseEntity<>(responseMap, status);
	}


	private void initMySuiteListener(Map<String, Object> request, Map<String, Object> responseMap){
		try {
			Class<?> cliDataProviderClazz = Class.forName("com.test.api.listeners.MySuiteListener");
			Object cliDataProvider = cliDataProviderClazz.newInstance();
			Method onStart = cliDataProviderClazz.getDeclaredMethod("onStart", ISuite.class);
			try {
				ISuite iSuite = this.createISuite(request);
				testNgService.setiSuite(iSuite);
				onStart.setAccessible(true);
				onStart.invoke(cliDataProvider, iSuite);
			} catch (Exception e) {
				String exceptionStackTrace = ExceptionUtils.getStackTrace(e);
				if(responseMap.containsKey("error")){
					List<String> exp = (List<String>)responseMap.get("error");
					exp.add(exceptionStackTrace);
					responseMap.put("error",exp);
				}else{
					responseMap.put("error", Arrays.asList(exceptionStackTrace));
				}
			}
		} catch (Throwable e) {
			responseMap.put("error", e);
		}
	}

	@SuppressWarnings("unchecked")
	private ISuite createISuite(Map<String, Object> request){
		XmlSuite xmlSuite = new XmlSuite();
		ArrayList<String> includeGroupList = new ArrayList<>();
		xmlSuite.setIncludedGroups(includeGroupList);
		Map<String,String> suiteparameters = (Map<String,String>) request.get("suiteparameters");
		xmlSuite.setParameters(suiteparameters);
		Configuration configuration = new Configuration();
		System.setProperty("testng.order","none");
		xmlSuite.setParallel(ParallelMode.NONE);
		Comparator<ITestNGMethod> comparator = org.testng.internal.Systematiser.getComparator();
		SuiteRunner iSuite = new SuiteRunner(configuration, xmlSuite, "",comparator);
		return iSuite;
	}

	private Class<?> getClazz(String currentClassName, Map<String, Object> response){
		Class<?> clazz = null;
		try {
			return Class.forName(currentClassName);
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
		response.put("error", "Class "+currentClassName+" NOT Found in JVM");
		return clazz;
	}


	private Method getMethod(Class<?> clazz, String apiName, Map<String,Object> response){
		Method method = null;
		Method[] allMethods = clazz.getDeclaredMethods();
		for (Method currentMethod : allMethods) {
			currentMethod.setAccessible(true);
			//Note: Annotation check is kept because in Test-API method with same name might be present
			if(currentMethod.isAnnotationPresent(Test.class) && currentMethod.getName().equals(apiName)){
				return currentMethod;
			}
		}
		response.put("error", "Method with name "+apiName+" NOT FOUND");
		return method;
	}
}
