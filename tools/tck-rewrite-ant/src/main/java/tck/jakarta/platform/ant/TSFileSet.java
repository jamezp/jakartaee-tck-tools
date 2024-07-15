package tck.jakarta.platform.ant;

import java.util.Arrays;
import java.util.List;

/**
 * fileset and zipfileset
 * org.apache.tools.ant.types.FileSet
 * org.apache.tools.ant.types.ZipFileSet
 */
public class TSFileSet {
    String dir;
    String prefix;
    List<String> includes;

    /**
     * Build up the fileset from the attributes on the fileset element
     * @param fileset - a fileset configuration map
     */
    TSFileSet(AttributeMap fileset) {
        this.dir = fileset.getAttribute("dir");
        this.prefix = fileset.getAttribute("prefix");
        String includes = fileset.getAttribute("includes");
        String[] asArray = includes.split(",");
        this.includes = Arrays.asList(asArray);
    }
    public TSFileSet(String dir, String prefix, List<String> includes) {
        this.dir = dir;
        this.prefix = prefix;
        this.includes = includes;
    }

    @Override
    public String toString() {
        return "FileSet{" +
                "dir='" + dir + '\'' +
                ", prefix='" + prefix + '\'' +
                ", includes=" + includes +
                '}';
    }
}
