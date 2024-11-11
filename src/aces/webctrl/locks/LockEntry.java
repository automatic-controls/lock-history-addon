package aces.webctrl.locks;
import java.util.*;
import com.controlj.green.addonsupport.web.*;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
import javax.servlet.http.*;
public class LockEntry implements Comparable<LockEntry> {
  public volatile long epochSeconds;
  public volatile String operatorDisplayName;
  public volatile String operatorUsername;
  public volatile String locationDisplayName;
  public volatile String locationRefName;
  public volatile String pointRefName;
  public volatile String attribute;
  public volatile String oldValue;
  public volatile String newValue;
  private volatile boolean lockFlag;
  private volatile String key;
  private volatile String locationLink = "#";
  private volatile String pointLink = "#";
  public LockEntry(long epochSeconds, String operatorDisplayName, String operatorUsername, String locationDisplayName, String locationRefName){
    this.epochSeconds = epochSeconds;
    this.operatorDisplayName = operatorDisplayName;
    this.operatorUsername = operatorUsername;
    this.locationDisplayName = locationDisplayName;
    this.locationRefName = locationRefName;
  }
  public LockEntry copy(String pointRefName, String attribute, String oldValue, String newValue){
    final LockEntry le = new LockEntry(epochSeconds,operatorDisplayName,operatorUsername,locationDisplayName,locationRefName);
    le.pointRefName = pointRefName;
    le.attribute = attribute;
    le.oldValue = oldValue;
    le.newValue = newValue;
    le.key = le.locationRefName+';'+le.pointRefName;
    le.lockFlag = le.attribute.equals("locked");
    return le;
  }
  @Override public int compareTo(LockEntry e){
    int x = locationRefName.compareTo(e.locationRefName);
    if (x!=0){
      return x;
    }
    x = pointRefName.compareTo(e.pointRefName);
    if (x!=0){
      return x;
    }
    return Long.compare(epochSeconds,e.epochSeconds);
  }
  private static boolean isParent(Location parent, Location child){
    try{
      while (!parent.equals(child)){
        if (!child.hasParent()){
          return false;
        }
        child = child.getParent();
      }
      return true;
    }catch(UnresolvableException e){
      return false;
    }
  }
  public static int formatLinks(ArrayList<LockEntry> arr, HttpServletRequest req, final String locref) throws Throwable {
    final Container<Integer> kills = new Container<Integer>(0);
    DirectAccess.getDirectAccess().getRootSystemConnection().runReadAction(new ReadAction(){
      @Override public void execute(SystemAccess sys){
        final Location geoLoc = sys.getGeoRoot();
        final Node geo = geoLoc.toNode();
        final Tree tree = sys.getTree(SystemTree.Geographic);
        Location baseLoc = null;
        String base = locref.trim();
        if (!base.isEmpty()){
          try{
            baseLoc = geo.evalToNode(base).resolveToLocation(tree);
          }catch(Throwable t){}
        }
        Location loc, loc2;
        int i,j;
        boolean err = true;
        boolean tested;
        LockEntry le;
        for (i=arr.size()-1;i>=0;--i){
          le = arr.get(i);
          tested = false;
          try{
            loc = geo.evalToNode(le.locationRefName).resolveToLocation(tree);
            if (loc!=null){
              tested = true;
              if (baseLoc!=null && !isParent(baseLoc, loc)){
                arr.set(i,null);
                ++kills.x;
                continue;
              }
              if (baseLoc!=null){
                le.locationDisplayName = loc.getRelativeDisplayPath(baseLoc);
              }else{
                le.locationDisplayName = loc.getRelativeDisplayPath(geoLoc);
              }
              le.locationLink = Link.createLink(UITree.GEO, loc).getURL(req);
              j = le.pointRefName.indexOf('/');
              if (j==-1){
                loc2 = loc.getDescendant(le.pointRefName);
                if (loc2!=null){
                  le.pointRefName = loc2.getRelativeDisplayPath(loc);
                  le.pointLink = Link.createLink(UITree.GEO, loc2).getURL(req);
                }
              }else{
                loc2 = loc.getDescendant(le.pointRefName.substring(0,j));
                if (loc2!=null){
                  le.pointRefName = loc2.getRelativeDisplayPath(loc)+' '+le.pointRefName.substring(j);
                  le.pointLink = Link.createLink(UITree.GEO, loc2).getURL(req);
                }
              }
            }
          }catch(Throwable t){
            if (!tested && baseLoc!=null){
              arr.set(i,null);
              ++kills.x;
            }
            if (err){
              err = false;
              Initializer.log(t);
            }
          }
        }
      }
    });
    return kills.x;
  }
  public static ArrayList<LockEntry> filterLastEntry(final ArrayList<LockEntry> arr){
    //kill all locked_value except the last for a given location
    //kill all locked except the last for a given location
    int kills = 0;
    {
      final HashSet<String> locked = new HashSet<String>();
      final HashSet<String> locked_value = new HashSet<String>();
      LockEntry le;
      for (int i=arr.size()-1;i>=0;--i){
        le = arr.get(i);
        if (le.lockFlag){
          if (!locked.add(le.key)){
            arr.set(i,null);
            ++kills;
          }
        }else{
          if (!locked_value.add(le.key)){
            arr.set(i,null);
            ++kills;
          }
        }
      }
    }
    final ArrayList<LockEntry> arr2 = new ArrayList<LockEntry>(arr.size()-kills);
    for (LockEntry le: arr){
      if (le!=null){
        arr2.add(le);
      }
    }
    return arr2;
  }
  public static ArrayList<LockEntry> filterCurrentlyLockedPoints(final ArrayList<LockEntry> arr, final boolean requiresSort){
    ArrayList<LockEntry> sortedArr;
    if (requiresSort){
      sortedArr = new ArrayList<LockEntry>(arr);
      sortedArr.sort(null);
    }else{
      sortedArr = arr;
    }
    final HashSet<String> currentLocks = new HashSet<String>();
    {
      String prevValue = null;
      String prevKey = null;
      for (LockEntry le: sortedArr){
        if (le.lockFlag){
          if (prevKey!=null && !le.key.equals(prevKey) && prevValue.equalsIgnoreCase("true")){
            currentLocks.add(prevKey);
          }
          prevKey = le.key;
          prevValue = le.newValue;
        }
      }
      if (prevKey!=null && prevValue.equalsIgnoreCase("true")){
        currentLocks.add(prevKey);
      }
    }
    final ArrayList<LockEntry> arr2 = new ArrayList<LockEntry>(arr.size());
    for (LockEntry le: arr){
      if (currentLocks.contains(le.key)){
        arr2.add(le);
      }
    }
    return arr2;
  }
  public static StringBuilder toString(final StringBuilder sb, final ArrayList<LockEntry> arr){
    sb.append('[');
    boolean first = true;
    for (LockEntry le: arr){
      if (le!=null){
        if (first){
          first = false;
        }else{
          sb.append(',');
        }
        le.toString(sb);
      }
    }
    sb.append(']');
    return sb;
  }
  public StringBuilder toString(final StringBuilder sb){
    sb.append('{');
    sb.append("\"epochSeconds\":").append(epochSeconds).append(',');
    sb.append("\"operator\":\"").append(Utility.escapeJSON(operatorDisplayName)).append("\",");
    sb.append("\"location\":\"").append(Utility.escapeJSON(locationDisplayName)).append("\",");
    sb.append("\"locationLink\":\"").append(Utility.escapeJSON(locationLink)).append("\",");
    sb.append("\"pointName\":\"").append(Utility.escapeJSON(pointRefName)).append("\",");
    sb.append("\"pointLink\":\"").append(Utility.escapeJSON(pointLink)).append("\",");
    sb.append("\"attribute\":\"").append(Utility.escapeJSON(attribute)).append("\",");
    sb.append("\"oldValue\":\"").append(Utility.escapeJSON(oldValue)).append("\",");
    sb.append("\"newValue\":\"").append(Utility.escapeJSON(newValue)).append("\"");
    sb.append('}');
    return sb;
  }
}