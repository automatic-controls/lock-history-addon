package aces.webctrl.locks;
import javax.servlet.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.format.*;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.function.*;
import com.controlj.green.addonsupport.*;
import com.controlj.green.core.main.*;
public class Initializer implements ServletContextListener {
  /** Contains basic information about this addon */
  public volatile static AddOnInfo info = null;
  /** The name of this addon */
  private volatile static String name;
  /** Prefix used for constructing relative URL paths */
  private volatile static String prefix;
  /** Path to the private directory for this addon */
  //private volatile static Path root;
  /** Path to the system directory */
  private volatile static Path system;
  /** Logger for this addon */
  private volatile static FileLogger logger;
  private final static DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
  private final static Pattern auditLogFilenamePattern = Pattern.compile("^auditlog.*\\.txt$");
  //^([\w:]+(?: {1,2}[\w:]+)+) +([^\(\s]+(?: +[^\(\s]+)*) +\(([^\)\n]+)\) +"(?!-")([^"\n]+)" +\((?!-\))([^\)\n]+)\)
  //^([\w:]++(?: {1,2}[\w:]++)++) ++([^\(\s]++(?: ++[^\(\s]++)*+) ++\(([^\)]++)\) ++"(?!-")([^"]++)" ++\((?!-\))([^\)]++)\)
  private final static Pattern entryHeaderPattern = Pattern.compile("^([\\w:]++(?: {1,2}[\\w:]++)++) ++([^\\(\\s]++(?: ++[^\\(\\s]++)*+) ++\\(([^\\)]++)\\) ++\"(?!-\")([^\"]++)\" ++\\((?!-\\))([^\\)]++)\\)");
  //^ +([^/\n]+(?:/(?!locked(?:_value)? )[^/\n]+)*)/(locked(?:_value)?) +[^"\n]+"([^"\n]*)" +[^"\n]+"([^"\n]*)"
  //^ ++([^/]++(?:/(?!locked(?:_value)?+ )[^/]++)*+)/(locked(?:_value)?+) ++[^"]++"([^"]*+)" ++[^"]++"([^"]*+)"
  private final static Pattern entryDataPattern = Pattern.compile("^ ++([^/]++(?:/(?!locked(?:_value)?+ )[^/]++)*+)/(locked(?:_value)?+) ++[^\"]++\"([^\"]*+)\" ++[^\"]++\"([^\"]*+)\"");
  /**
   * Entry point of this add-on.
   */
  @Override public void contextInitialized(ServletContextEvent sce){
    info = AddOnInfo.getAddOnInfo();
    name = info.getName();
    prefix = '/'+name+'/';
    //root = info.getPrivateDir().toPath();
    logger = info.getDateStampLogger();
    system = Core.getSystemDirectory().toPath();
  }
  /**
   * Releases all resources.
   */
  @Override public void contextDestroyed(ServletContextEvent sce){}
  /**
   * Parses a timestamp of the following format: {@code EEE MMM dd HH:mm:ss zzz yyyy}.
   * @return Epoch seconds, which is the count of elapsed seconds since {@code 1970-01-01T00:00Z}.
   */
  public static long parseTimestamp(String ts) throws DateTimeParseException {
    return timestampFormat.parse(ts).getLong(ChronoField.INSTANT_SECONDS);
  }
  /**
   * @param after All audit logs must have lastModifiedTime greater than this value in epoch milliseconds.
   * @return a list of audit logs ordered ascending by last modification date.
   */
  public static ArrayList<PathStamp> getAuditLogs(long after) throws IOException {
    final ArrayList<PathStamp> list = new ArrayList<PathStamp>();
    try(
      Stream<Path> stream = Files.find(system, 1, new BiPredicate<Path,BasicFileAttributes>() {
        @Override public boolean test(Path p, BasicFileAttributes attr){
          if (attr.isRegularFile() && auditLogFilenamePattern.matcher(p.getFileName().toString()).matches()){
            final long millis = attr.lastModifiedTime().toMillis();
            if (millis>after){
              list.add(new PathStamp(p, millis));
              return true;
            }
          }
          return false;
        }
      });
    ){
      stream.forEach(new Consumer<Path>(){
        @Override public void accept(Path p){}
      });
    }
    list.sort(null);
    return list;
  }
  /**
   * @param after specifies to only consider entries with timestamp greater than this value in epoch milliseconds.
   * @return a list of lock attribute edits found in the auditlog sorted by timestamp ascending. Use {@code ArrayList<LockEntry>.sort(null)} to order by location.
   */
  public synchronized static ArrayList<LockEntry> parseAuditLog(long after) throws IOException {
    final ArrayList<PathStamp> arr = getAuditLogs(after);
    final ArrayList<LockEntry> entries = new ArrayList<LockEntry>(2048);
    final long lim = after/1000;
    final Container<LockEntry> base = new Container<LockEntry>();
    for (PathStamp ps: arr){
      try(
        Stream<String> stream = Files.lines(ps.p);
      ){
        stream.forEachOrdered(new Consumer<String>() {
          @Override public void accept(final String s){
            final boolean space = s.startsWith(" ");
            if (base.x!=null && space){
              final Matcher m = entryDataPattern.matcher(s);
              if (m.find()){
                entries.add(base.x.copy(m.group(1), m.group(2), m.group(3), m.group(4)));
              }
            }else if (!space){
              final Matcher m = entryHeaderPattern.matcher(s);
              if (m.find()){
                try{
                  final long epochSeconds = parseTimestamp(m.group(1));
                  if (epochSeconds>lim){
                    base.x = new LockEntry(epochSeconds, m.group(2), m.group(3), m.group(4), m.group(5));
                  }else{
                    base.x = null;
                  }
                }catch(DateTimeParseException e){
                  base.x = null;
                  log(e);
                }
              }else{
                base.x = null;
              }
            }
          }
        });
      }
    }
    return entries;
  }
  /**
   * @return the name of this application.
   */
  public static String getName(){
    return name;
  }
  /**
   * @return the prefix used for constructing relative URL paths.
   */
  public static String getPrefix(){
    return prefix;
  }
  /**
   * Logs a message.
   */
  public synchronized static void log(String str){
    logger.println(str);
  }
  /**
   * Logs an error.
   */
  public synchronized static void log(Throwable t){
    logger.println(t);
  }
}