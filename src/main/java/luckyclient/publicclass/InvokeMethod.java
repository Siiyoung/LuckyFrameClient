package luckyclient.publicclass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import luckyclient.caserun.publicdispose.ChangString;
import luckyclient.caserun.publicdispose.ParamsManageForSteps;
import luckyclient.publicclass.remoterinterface.HttpClientHelper;
import luckyclient.serverapi.api.GetServerAPI;
import luckyclient.serverapi.entity.ProjectProtocolTemplate;
import luckyclient.serverapi.entity.ProjectTemplateParams;

/**
 * =================================================================
 * 这是一个受限制的自由软件！您不能在任何未经允许的前提下对程序代码进行修改和用于商业用途；也不允许对程序代码修改后以任何形式任何目的的再发布。
 * 为了尊重作者的劳动成果，LuckyFrame关键版权信息严禁篡改
 * 有任何疑问欢迎联系作者讨论。 QQ:1573584944  seagull1985
 * =================================================================
 *
 * @ClassName: InvokeMethod
 * @Description: 动态调用方法
 * @author： seagull
 * @date 2017年9月24日 上午9:29:40
 */
public class InvokeMethod {

    /**
     * 动态调用JAVA
     * @param packagename
     * @param functionname
     * @param getParameterValues
     * @param steptype
     * @param action
     * @return
     */
    public static String callCase(String packagename, String functionname, Object[] getParameterValues, int steptype, String extend) {
        String result = "调用异常，请查看错误日志！";
        try {
            if (steptype == 2) {                
                if(functionname.toLowerCase().endsWith(".py")){
                	//调用Python脚本
                	LogUtil.APP.info("准备开始调用Python脚本......");
                	result = callPy(packagename, functionname, getParameterValues);
                }else{
                	//调用JAVA
                    // 调用非静态方法用到
                	LogUtil.APP.info("准备开始调用JAVA驱动桩程序......");
                    Object server = Class.forName(packagename).newInstance();
                    @SuppressWarnings("rawtypes")
                    Class[] getParameterTypes = null;
                    if (getParameterValues != null) {
                        int paramscount = getParameterValues.length;
                        // 赋值数组，定义类型
                        getParameterTypes = new Class[paramscount];
                        for (int i = 0; i < paramscount; i++) {
                            getParameterTypes[i] = String.class;
                        }
                    }
                    Method method = getMethod(server.getClass().getMethods(), functionname, getParameterTypes);
                    if (method == null) {
                        throw new Exception("客户端本地驱动目录下没有在包名为【" + packagename + "】中找到被调用的方法【" + functionname + "】,请检查方法名称以及参数个数是否一致！");
                    }
                    Object str = method.invoke(server, getParameterValues);
                    if (str == null) {
                        result = "调用异常，返回结果是null";
                    } else {
                        result = str.toString();
                    }
                }
            } else if (steptype == 0) {
            	if(null==extend||"".equals(extend)||!extend.contains("】")){
            		result = "您当前步骤是HTTP请求，请确认是否没有配置对应的HTTP协议模板...";
            		LogUtil.APP.warn("您当前步骤是HTTP请求，请确认是否没有配置对应的HTTP协议模板...");
            		return result;
            	}
                String templateidstr = extend.substring(1, extend.indexOf("】"));
                String templatenamestr = extend.substring(extend.indexOf("】") + 1);
                LogUtil.APP.info("即将使用模板【{}】，ID:【{}】发送HTTP请求！",templatenamestr,templateidstr);

                ProjectProtocolTemplate ppt = GetServerAPI.clientGetProjectProtocolTemplateByTemplateId(Integer.valueOf(templateidstr));
                if (null == ppt) {
                    LogUtil.APP.warn("协议模板为空，请检查用例使用的协议模板是否已经删除！");
                    return "协议模板为空，请确认用例使用的模板是否已经删除！";
                }

                List<ProjectTemplateParams> paramslist = GetServerAPI.clientGetProjectTemplateParamsListByTemplateId(Integer.valueOf(templateidstr));

                //处理头域
                Map<String, String> headmsg = new HashMap<String, String>(0);
                if (null != ppt.getHeadMsg() && !ppt.getHeadMsg().equals("") && ppt.getHeadMsg().indexOf("=") > 0) {
                    String headmsgtemp = ppt.getHeadMsg().replace("\\;", "!!!fhzh");
                    String[] temp = headmsgtemp.split(";", -1);
                    for (int i = 0; i < temp.length; i++) {
                        if (null != temp[i] && !temp[i].equals("") && temp[i].indexOf("=") > 0) {
                            String key = temp[i].substring(0, temp[i].indexOf("="));
                            String value = temp[i].substring(temp[i].indexOf("=") + 1);
                            value = value.replace("!!!fhzh",";");
                            value = ParamsManageForSteps.paramsManage(value);
                            headmsg.put(key, value);
                        }
                    }
                }

                //处理更换参数
                if (null != getParameterValues) {
                    String booleanheadmsg = "headmsg(";
                    String msgend = ")";
                    for (Object obp : getParameterValues) {
                        String paramob = obp.toString();
                        if(paramob.contains("#")){
                            String key = paramob.substring(0, paramob.indexOf("#"));
                            String value = paramob.substring(paramob.indexOf("#") + 1);
                            value = ParamsManageForSteps.paramsManage(value);
                            if (key.contains(booleanheadmsg) && key.contains(msgend)) {
                                String head = key.substring(key.indexOf(booleanheadmsg) + 8, key.lastIndexOf(msgend));
                                headmsg.put(head, value);
                                continue;
                            }
                            int replaceflag=0;
                            for (int i = 0; i < paramslist.size(); i++) {
                                ProjectTemplateParams ptp = paramslist.get(i);
                                ptp.setParamValue(ParamsManageForSteps.paramsManage(ptp.getParamValue()));
                                if("_forTextJson".equals(ptp.getParamName())){
                            		//分析参数替换序号
                            		int index = 1;
                            		if (key.contains("[") && key.endsWith("]")) {
                            			index = Integer.valueOf(key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]")));
                            			key = key.substring(0, key.lastIndexOf("["));
                            			LogUtil.APP.info("准备替换JSON对象中的参数值，替换指定第{}个参数...",index);
                            		} else {
                            			LogUtil.APP.info("准备替换JSON对象中的参数值，未检测到指定参数名序号，默认替换第1个参数...");                       			
                            		}
                            		
                                	if(ptp.getParamValue().contains("\""+key+"\":")){
                                		Map<String,String> map=ChangString.changjson(ptp.getParamValue(), key, value,index);
                                		if("true".equals(map.get("boolean"))){
                                            ptp.setParamValue(map.get("json"));
                                            paramslist.set(i, ptp);
                                            replaceflag=1;
                                            LogUtil.APP.info("替换参数【{}】完成...",key);
                                            break;
                                		}
                                	}else if(ptp.getParamValue().contains(key)){
                                		ptp.setParamValue(ptp.getParamValue().replace(key, value));
                                		paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("检查当前文本不属于JSON,在字符串【{}】中直接把【{}】替换成【{}】...",ptp.getParamValue(),key,value);
                                        break;
                                	}else{
                                		LogUtil.APP.warn("请检查您的纯文本模板是否是正常的JSON格式或是文本中是否存在需替换的关键字。");
                                	}
                                }else{
                                    if (ptp.getParamName().equals(key)) {
                                        ptp.setParamValue(value);
                                        paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("把模板中参数【{}】的值设置成【{}】",key,value);
                                        break;
                                    }
                                }
                            }
                            if(replaceflag==0){
                            	LogUtil.APP.warn("步骤参数【{}】没有在模板中找到可替换的参数对应默认值，"
                            			+ "设置请求参数失败，请检查协议模板中此参数是否存在。",key);
                            }
                        }else{
                        	LogUtil.APP.warn("替换模板或是头域参数失败，原因是因为没有检测到#，"
                        			+ "注意HTTP请求替换参数格式是【headmsg(头域名#头域值)|参数名#参数值|参数名2#参数值2】");
                        }

                    }
                }
                //处理参数
                Map<String, Object> params = new HashMap<String, Object>(0);
                for (ProjectTemplateParams ptp : paramslist) {
                	String tempparam = "";
                	if(null!=ptp.getParamValue()){
                		tempparam =  ptp.getParamValue().replace("&quot;", "\"");
                	}else{
                		break;
                	}
                    //处理参数对象
                    if (ptp.getParamType() == 1) {
                        JSONObject json = JSONObject.parseObject(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), json);
                        LogUtil.APP.info("模板参数【{}】  JSONObject类型参数值:【{}】",ptp.getParamName(),json.toString());
                    } else if (ptp.getParamType() == 2) {
                        JSONArray jarr = JSONArray.parseArray(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), jarr);
                        LogUtil.APP.info("模板参数【{}】  JSONArray类型参数值:【{}】",ptp.getParamName(),jarr.toString());
                    } else if (ptp.getParamType() == 3) {
                        File file = new File(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), file);
                        LogUtil.APP.info("模板参数【{}】  File类型参数值:【{}】",ptp.getParamName(),file.getAbsolutePath());
                    } else if (ptp.getParamType() == 4) {
                        Double dp = Double.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), dp);
                        LogUtil.APP.info("模板参数【{}】  数字类型参数值:【{}】",ptp.getParamName(),tempparam);
                    } else if (ptp.getParamType() == 5) {
                        Boolean bn = Boolean.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), bn);
                        LogUtil.APP.info("模板参数【{}】  Boolean类型参数值:【{}】",ptp.getParamName(),bn);
                    } else {
                        params.put(ptp.getParamName().replace("&quot;", "\""), ptp.getParamValue().replace("&quot;", "\""));
                        LogUtil.APP.info("模板参数【{}】  String类型参数值:【{}】",ptp.getParamName(),ptp.getParamValue().replace("&quot;", "\""));
                    }
                }

                if (functionname.toLowerCase().equals("httpurlpost")) {
                    result = HttpClientHelper.sendHttpURLPost(packagename, params,headmsg,ppt);
                } else if (functionname.toLowerCase().equals("urlpost")) {
                    result = HttpClientHelper.sendURLPost(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("getandsavefile")) {
                    String fileSavePath = System.getProperty("user.dir") + "\\HTTPSaveFile\\";
                    result = HttpClientHelper.sendGetAndSaveFile(packagename, params, fileSavePath, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpurlget")) {
                    result = HttpClientHelper.sendHttpURLGet(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("urlget")) {
                    result = HttpClientHelper.sendURLGet(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpclientpost")) {
                    result = HttpClientHelper.httpClientPost(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientuploadfile")) {
                    result = HttpClientHelper.httpClientUploadFile(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientpostjson")) {
                    result = HttpClientHelper.httpClientPostJson(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpurldelete")) {
                    result = HttpClientHelper.sendHttpURLDel(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpclientputjson")) {
                    result = HttpClientHelper.httpClientPutJson(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientput")) {
                    result = HttpClientHelper.httpClientPut(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientget")) {
                    result = HttpClientHelper.httpClientGet(packagename, params, headmsg, ppt);
                } else {
                    LogUtil.APP.warn("您的HTTP操作方法异常，检测到的操作方法是:{}",functionname);
                    result = "调用异常，请查看错误日志！";
                }
            } else if (steptype == 4) {
                String templateidstr = extend.substring(1, extend.indexOf("】"));
                String templatenamestr = extend.substring(extend.indexOf("】") + 1);
                LogUtil.APP.info("即将使用模板【{}】，ID:【{}】 发送SOCKET请求！",templatenamestr,templateidstr);

                ProjectProtocolTemplate ppt = GetServerAPI.clientGetProjectProtocolTemplateByTemplateId(Integer.valueOf(templateidstr));
                if (null == ppt) {
                    LogUtil.APP.warn("协议模板为空，请检查用例使用的协议模板是否已经删除！");
                    return "协议模板为空，请确认用例使用的模板是否已经删除！";
                }
                
                List<ProjectTemplateParams> paramslist = GetServerAPI.clientGetProjectTemplateParamsListByTemplateId(Integer.valueOf(templateidstr));
                
                //处理头域
                Map<String, String> headmsg = new HashMap<String, String>(0);
                if (null != ppt.getHeadMsg() && !ppt.getHeadMsg().equals("") && ppt.getHeadMsg().indexOf("=") > 0) {
                    String headmsgtemp = ppt.getHeadMsg().replace("\\;", "!!!fhzh");
                    String[] temp = headmsgtemp.split(";", -1);
                    for (int i = 0; i < temp.length; i++) {
                        if (null != temp[i] && !temp[i].equals("") && temp[i].indexOf("=") > 0) {
                            String key = temp[i].substring(0, temp[i].indexOf("="));
                            String value = temp[i].substring(temp[i].indexOf("=") + 1);
                            value = value.replace("!!!fhzh",";");
                            headmsg.put(key, value);
                        }
                    }
                }

                //处理更换参数
                if (null != getParameterValues) {
                    String booleanheadmsg = "headmsg(";
                    String msgend = ")";
                    for (Object obp : getParameterValues) {
                        String paramob = obp.toString();
                        if(paramob.contains("#")){
                            String key = paramob.substring(0, paramob.indexOf("#"));
                            String value = paramob.substring(paramob.indexOf("#") + 1);
                            if (key.contains(booleanheadmsg) && key.contains(msgend)) {
                                String head = key.substring(key.indexOf(booleanheadmsg) + 8, key.lastIndexOf(msgend));
                                headmsg.put(head, value);
                                continue;
                            }
                            int replaceflag=0;
                            for (int i = 0; i < paramslist.size(); i++) {
                                ProjectTemplateParams ptp = paramslist.get(i);
                                if("_forTextJson".equals(ptp.getParamName())){
                                	if(ptp.getParamValue().indexOf("\""+key+"\":")>=0){
                                 		//分析参数替换序号
                                		int index = 1;
                                		if (key.indexOf("[") >= 0 && key.endsWith("]")) {
                                			index = Integer.valueOf(key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]")));
                                			key = key.substring(0, key.lastIndexOf("["));
                                			LogUtil.APP.info("准备替换JSON对象中的参数值，未检测到指定参数名序号，默认替换第1个参数...");
                                		} else {
                                			LogUtil.APP.info("准备替换JSON对象中的参数值，替换指定第【{}】个参数...",index);
                                		}
                                		
                                		Map<String,String> map=ChangString.changjson(ptp.getParamValue(), key, value,index);
                                		if("true".equals(map.get("boolean"))){
                                            ptp.setParamValue(map.get("json"));
                                            paramslist.set(i, ptp);
                                            replaceflag=1;
                                            LogUtil.APP.info("替换参数【{}】完成...",key);
                                            break;
                                		}
                                	}else if(ptp.getParamValue().indexOf(key)>=0){
                                		ptp.setParamValue(ptp.getParamValue().replace(key, value));
                                		paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("检查当前文本不属于JSON,在字符串【{}】中直接把【{}】替换成【{}】...",ptp.getParamValue(),key,value);
                                        break;
                                	}else{
                                		LogUtil.APP.warn("请检查您的纯文本模板是否是正常的JSON格式或是文本中是否存在需替换的关键字。");
                                	}
                                }else{
                                    if (ptp.getParamName().equals(key)) {
                                        ptp.setParamValue(value);
                                        paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("把模板中参数【{}】的值设置成【{}】",key,value);
                                        break;
                                    }
                                }
                            }
                            if(replaceflag==0){
                            	LogUtil.APP.warn("步骤参数【{}】没有在模板中找到可替换的参数对应默认值，"
                            			+ "设置请求参数失败，请检查协议模板中此参数是否存在。",key);
                            }
                        }else{
                        	LogUtil.APP.warn("替换模板或是头域参数失败，原因是因为没有检测到#，"
                        			+ "注意HTTP请求替换参数格式是【headmsg(头域名#头域值)|参数名#参数值|参数名2#参数值2】");
                        }

                    }
                }
                //处理参数
                Map<String, Object> params = new HashMap<String, Object>(0);
                for (ProjectTemplateParams ptp : paramslist) {
                	String tempparam = "";
                	if(null!=ptp.getParamValue()){
                		tempparam =  ptp.getParamValue().replace("&quot;", "\"");
                	}
                    //处理参数对象
                    if (ptp.getParamType() == 1) {
                        JSONObject json = JSONObject.parseObject(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), json);
                        LogUtil.APP.info("模板参数【{}】  JSONObject类型参数值:【{}】",ptp.getParamName(),json.toString());
                    } else if (ptp.getParamType() == 2) {
                        JSONArray jarr = JSONArray.parseArray(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), jarr);
                        LogUtil.APP.info("模板参数【{}】  JSONArray类型参数值:【{}】",ptp.getParamName(),jarr.toString());
                    } else if (ptp.getParamType() == 3) {
                        File file = new File(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), file);
                        LogUtil.APP.info("模板参数【{}】  File类型参数值:【{}】",ptp.getParamName(),file.getAbsolutePath());
                    } else if (ptp.getParamType() == 4) {
                        Double dp = Double.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), dp);
                        LogUtil.APP.info("模板参数【{}】  数字类型参数值:【{}】",ptp.getParamName(),tempparam);
                    } else if (ptp.getParamType() == 5) {
                        Boolean bn = Boolean.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), bn);
                        LogUtil.APP.info("模板参数【{}】  Boolean类型参数值:【{}】",ptp.getParamName(),bn);
                    } else {
                        params.put(ptp.getParamName().replace("&quot;", "\""), ptp.getParamValue().replace("&quot;", "\""));
                        LogUtil.APP.info("模板参数【{}】  String类型参数值:【{}】",ptp.getParamName(),ptp.getParamValue().replace("&quot;", "\""));
                    }
                }


                if (functionname.toLowerCase().equals("socketpost")) {
                    result = HttpClientHelper.sendSocketPost(packagename, params, ppt.getEncoding().toLowerCase(), headmsg);
                } else if (functionname.toLowerCase().equals("socketget")) {
                    result = HttpClientHelper.sendSocketGet(packagename, params, ppt.getEncoding().toLowerCase(), headmsg);
                } else {
                    LogUtil.APP.warn("您的SOCKET操作方法异常，检测到的操作方法是:{}",functionname);
                    result = "调用异常，请查看错误日志！";
                }
            }
        } catch (Throwable e) {
            LogUtil.APP.error("调用异常，请查看错误日志！", e);
            return "调用异常，请查看错误日志！";
        }
        return result;
    }

    public static Method getMethod(Method[] methods, String methodName, @SuppressWarnings("rawtypes") Class[] parameterTypes) {
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals(methodName)) {
                continue;
            }
            if (compareParameterTypes(parameterTypes, methods[i].getParameterTypes())) {
                return methods[i];
            }

        }
        return null;
    }

    public static boolean compareParameterTypes(@SuppressWarnings("rawtypes") Class[] parameterTypes, @SuppressWarnings("rawtypes") Class[] orgParameterTypes) {
        // parameterTypes 里面，int->Integer
        // orgParameterTypes是原始参数类型
        if (parameterTypes == null && orgParameterTypes == null) {
            return true;
        }
        if (parameterTypes == null && orgParameterTypes != null) {
            if (orgParameterTypes.length == 0) {
                return true;
            } else {
                return false;
            }
        }
        if (parameterTypes != null && orgParameterTypes == null) {
            if (parameterTypes.length == 0) {
                return true;
            } else {
                return false;
            }

        }
        if (parameterTypes.length != orgParameterTypes.length) {
            return false;
        }

        return true;
    }
    
    /**
     * 调用Python脚本
     * @param packagename
     * @param functionname
     * @param getParameterValues
     * @return
     */
    private static String callPy(String packagename, String functionname, Object[] getParameterValues){
    	String result = "调用Python脚本过程异常，返回结果是null";
    	try {
    		// 定义缓冲区、正常结果输出流、错误信息输出流
    		byte[] buffer = new byte[1024];
    		ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
    		ByteArrayOutputStream outerrStream = new ByteArrayOutputStream();
    		int params=0;
    		if(getParameterValues!=null){
    			params=getParameterValues.length;
    		}
    		String[] args = new String[2+params];
    		args[0]="python";
            if(packagename.endsWith(File.separator)){
            	args[1]=packagename+functionname;
            	//args[1]="E:\\PycharmProjects\\untitled\\venv\\testaaa.py";
            }else{
            	args[1]=packagename+File.separator+functionname;
            	//args[1]="E:\\PycharmProjects\\untitled\\venv\\testaaa.py";
            }
            LogUtil.APP.info("调用Python脚本路径:{}",args[1]);
    		for(int i=0;i < params;i++){
    			args[2+i]=getParameterValues[i].toString();
    		}

            Process proc=Runtime.getRuntime().exec(args);
            InputStream errStream = proc.getErrorStream();
            InputStream stream = proc.getInputStream();
            
            // 流读取与写入
            int len = -1;  
            while ((len = errStream.read(buffer)) != -1) {  
                outerrStream.write(buffer, 0, len);  
            }  
            while ((len = stream.read(buffer)) != -1) {  
                outStream.write(buffer, 0, len);  
            }
            
            proc.waitFor();
            // 打印流信息
            if(outerrStream.toString().equals("")){
            	result = outStream.toString().trim();
            	LogUtil.APP.info("成功调用Python脚本，返回结果:{}",result);
            }else{
            	result = outerrStream.toString().trim();
            	if(result.indexOf("ModuleNotFoundError")>-1){
            		LogUtil.APP.warn("调用Python脚本出现异常，有相关Python模块未引用到，请在Python脚本中注意设置系统环境路径(例: sys.path.append(\"E:\\PycharmProjects\\untitled\\venv\\Lib\\site-packages\"))，"
            				+ "详细错误信息:{}",result);
            	}else if(result.indexOf("No such file or directory")>-1){
            		LogUtil.APP.warn("调用Python脚本出现异常，在指定路径下未找到Python脚本，原因有可能是Python脚本路径错误或是传入Python指定参数个数不一致，详细错误信息:{}",result);
            	}else{
            		LogUtil.APP.warn("调用Python脚本出现异常，错误信息:{}",result);
            	}        	
            }
           } 
         catch (Exception e) {
        	 LogUtil.APP.error("调用Python脚本出现异常,请检查！",e);
            return result;
        }
    	return result;
    }

}
