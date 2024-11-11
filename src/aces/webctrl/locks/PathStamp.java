package aces.webctrl.locks;
import java.nio.file.*;
public class PathStamp implements Comparable<PathStamp> {
  public volatile Path p;
  public volatile long t;
  public PathStamp(Path p, long t){
    this.p = p;
    this.t = t;
  }
  @Override public int compareTo(PathStamp x){
    return Long.compare(t,x.t);
  }
}