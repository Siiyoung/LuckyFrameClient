package luckyclient.caserun;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;

import luckyclient.caserun.exinterface.TestControl;
import luckyclient.caserun.exinterface.WebTestCaseDebug;
import luckyclient.publicclass.LogUtil;

/**
 * =================================================================
 * 这是一个受限制的自由软件！您不能在任何未经允许的前提下对程序代码进行修改和用于商业用途；也不允许对程序代码修改后以任何形式任何目的的再发布。
 * 为了尊重作者的劳动成果，LuckyFrame关键版权信息严禁篡改
 * 有任何疑问欢迎联系作者讨论。 QQ:1573584944  seagull1985
 * =================================================================
 * 
 * @author： seagull
 * @date 2017年12月1日 上午9:29:40
 * 
 */
public class WebDebugExecute extends TestControl{

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		try {
			PropertyConfigurator.configure(System.getProperty("user.dir") + File.separator + "log4j.conf");
	 		String caseIdStr = args[0];
	 		String userIdStr = args[1];
	 		WebTestCaseDebug.oneCaseDebug(caseIdStr, userIdStr);
		} catch (Exception e) {
			// TODO: handle exception
			LogUtil.APP.error("启动用例调试主函数出现异常，请检查！",e);
		} finally{
			System.exit(0);
		}
	}
}
