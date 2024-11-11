package aces.webctrl.locks;
import javax.servlet.http.*;
import java.util.*;
public class MainPage extends ServletBase {
  @Override public void exec(final HttpServletRequest req, final HttpServletResponse res) throws Throwable {
    final String type = req.getParameter("type");
    if (type==null){
      res.setContentType("text/html");
      res.getWriter().print(getHTML(req));
    }else{
      switch (type){
        case "refresh":{
          final String milli = req.getParameter("milli");
          final String sortloc = req.getParameter("sortloc");
          final String onlylock = req.getParameter("onlylock");
          final String onlylast = req.getParameter("onlylast");
          final String loc = req.getParameter("loc");
          if (milli!=null && sortloc!=null && onlylock!=null && onlylast!=null && loc!=null){
            try{
              final long _milli = System.currentTimeMillis()-Long.parseLong(milli);
              final boolean _sortloc = sortloc.equalsIgnoreCase("true");
              final boolean _onlylock = onlylock.equalsIgnoreCase("true");
              final boolean _onlylast = onlylast.equalsIgnoreCase("true");
              ArrayList<LockEntry> arr = Initializer.parseAuditLog(_milli);
              if (_onlylast){
                arr = LockEntry.filterLastEntry(arr);
              }
              if (_sortloc){
                arr.sort(null);
              }
              if (_onlylock){
                arr = LockEntry.filterCurrentlyLockedPoints(arr, !_sortloc);
              }
              if (_sortloc){
                arr.sort(new Comparator<LockEntry>(){
                  @Override public int compare(LockEntry a, LockEntry b){
                    int x = a.locationRefName.compareTo(b.locationRefName);
                    if (x!=0){
                      return x;
                    }
                    x = a.pointRefName.compareTo(b.pointRefName);
                    if (x!=0){
                      return x;
                    }
                    return Long.compare(b.epochSeconds,a.epochSeconds);
                  }
                });
              }else{
                Collections.reverse(arr);
              }
              int kills = LockEntry.formatLinks(arr, req, loc);
              final String s = LockEntry.toString(new StringBuilder(Math.max(350*(arr.size()-kills),16)), arr).toString();
              res.setContentType("application/json");
              res.getWriter().print(s);
            }catch(NumberFormatException e){
              res.setStatus(400);
            }catch(Throwable t){
              Initializer.log(t);
              res.setStatus(500);
            }
          }else{
            res.setStatus(400);
          }
          break;
        }
        default:{
          res.sendError(400, "Unrecognized type parameter.");
        }
      }
    }
  }
}