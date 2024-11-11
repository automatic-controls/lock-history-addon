package aces.webctrl.locks;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
public abstract class ServletBase extends HttpServlet {
  private volatile String html = null;
  public abstract void exec(HttpServletRequest req, HttpServletResponse res) throws Throwable;
  @Override public void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    doPost(req,res);
  }
  @Override public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    try{
      req.setCharacterEncoding("UTF-8");
      res.setCharacterEncoding("UTF-8");
      res.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      exec(req,res);
    }catch(NumberFormatException e){
      Initializer.log(e);
      res.sendError(400, "Failed to parse number from string.");
    }catch(Throwable t){
      Initializer.log(t);
      if (!res.isCommitted()){
        res.reset();
        res.setCharacterEncoding("UTF-8");
        res.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setContentType("text/plain");
        res.setStatus(500);
        t.printStackTrace(res.getWriter());
      }
    }
  }
  public String getHTML(final HttpServletRequest req) throws Throwable {
    if (html==null){
      html = Utility.loadResourceAsString("aces/webctrl/locks/"+getClass().getSimpleName()+".html")
      .replace("href=\"../../../../root/webapp/main.css\"", "href=\"main.css\"")
      .replace("__DOCUMENTATION__", "https://github.com/automatic-controls/lock-history-addon");
    }
    return html.replace("__PREFIX__", req.getContextPath());
  }
  public static String getUsername(final HttpServletRequest req) throws Throwable {
    return DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName().toLowerCase();
  }
}