/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package org.netbeans.apitest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.junit.NbTestCase;

/**
 *
 * @author Jaroslav Tulach
 */
public class APITest extends NbTestCase {
    private static File workDir;
    
    public APITest(String s) {
        super(s);
    }

    @Override
    protected void setUp() throws Exception {
        clearWorkDir();
        
        
    }
    
    public void testGenericsOverridenType() throws Exception {
        String c1 =
            "package ahoj;" +
            "public class A<T> {" +
            "  public T get() { return null; }" +
            "  public class B extends A<String> {" +
            "    public String get() { return \"\"; }" +
            "  }\n" +
            "}";
        createFile(1, "A.java", c1);
        createFile(2, "A.java", c1);
        
        compareAPIs(1, 2);
    }
    
    
    public void testAntScript() throws Exception {
        String c1 =
            "package x;" +
            "public class C {" +
            "  private C() { }" +
            "}";
        createFile(1, "C.java", c1);
        String cc1 =
            "package x.ignore;" +
            "public class X {" +
            "  private X() { }" +
            "}";
        createFile(1, "X.java", cc1);
        String c2 =
            "package x;" +
            "public class C {" +
            "  public C() { }" +
            "}";
        createFile(2, "C.java", c2);
        
        compareAPIs(1, 2);
    }
    
    public void testSubpackage() throws Exception {
        String c1 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "}";
        createFile(1, "C.java", c1);
        String cc1 =
            "package x.y.ignore;" +
            "public class X {" +
            "  private X() { }" +
            "}";
        createFile(1, "X.java", cc1);
        String c2 =
            "package x.y;" +
            "public class C {" +
            "  public C() { }" +
            "}";
        createFile(2, "C.java", c2);
        
        compareAPIs(1, 2, "-Dcheck.package=x.y.*");
    }
    
    public void testStaticMethodsReportedAsMissing() throws Exception {
        String c1 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public static C getDefault() { return new C(); }" +
            "}";
        createFile(1, "C.java", c1);
        String c2 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public static C getDefault() { return new C(); }" +
            "}";
        createFile(2, "C.java", c2);
        
        compareAPIs(1, 2, "-Dcheck.package=x.y.*", "-Dcheck.type=check");
    }

    public void testStaticFieldWasReportedAsMissing() throws Exception {
        String c1 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public static C DEFAULT = new C();" +
            "}";
        createFile(1, "C.java", c1);
        String c2 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public static C DEFAULT = new C();" +
            "}";
        createFile(2, "C.java", c2);
        
        compareAPIs(1, 2, "-Dcheck.package=x.y.*", "-Dcheck.type=check");
    }

    public void testNonStaticMethodsReportedAsMissing() throws Exception {
        String c1 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public C getDefault() { return new C(); }" +
            "}";
        createFile(1, "C.java", c1);
        String c2 =
            "package x.y;" +
            "public class C {" +
            "  private C() { }" +
            "  public C getDefault() { return new C(); }" +
            "}";
        createFile(2, "C.java", c2);
        
        compareAPIs(1, 2, "-Dcheck.package=x.y.*", "-Dcheck.type=check");
    }
    
    public void testStrictCheckDiscoversAnAPIChange() throws Exception {
        String c1 =
            "package x;" +
            "public class C {" +
            "  private C() { }" + 
            "}";
        createFile(1, "C.java", c1);
        String cc1 =
            "package x.ignore;" +
            "public class X {" +
            "  private X() { }" +
            "}";
        createFile(1, "X.java", cc1);
        String c2 =
            "package x;" +
            "public class C {" +
            "  public void newMeth() { }" +
            "}";
        createFile(2, "C.java", c2);
        
        try {
            compareAPIs(1, 2, "-Dcheck.type=StrictCheck");
            fail("This should fail, the mode is strict and we see some changes");
        } catch (ExecuteUtils.ExecutionError err) {
            // ok
        }
    }
    
    public void testDeleteOfAMethodIsReported() throws Exception {
        String c1 =
            "package x;" +
            "public class C {" +
            "  public void x() { }" +
            "}";
        createFile(1, "C.java", c1);
        String c2 =
            "package x;" +
            "public class C {" +
            "  public C() { }" +
            "}";
        createFile(2, "C.java", c2);
        
        try {
            compareAPIs(1, 2, "-Dcheck.type=Check");
            fail("This comparition should fail");
        } catch (ExecuteUtils.ExecutionError err) {
            // ok
        }
    }
    
    protected final void createFile(int slot, String name, String content) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slot);
        File c1 = new File(d1, name);
        copy(content, c1);
    }
    
    protected final void compareAPIs(int slotFirst, int slotSecond, String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slotFirst);
        File d2 = new File(getWorkDir(), "dir" + slotSecond);
        
        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);
        
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(additionalArgs));
        args.add("-Ddir1=" + d1);
        args.add("-Ddir2=" + d2);
        ExecuteUtils.execute(build, args.toArray(new String[0]));
    }
    
    private static final void copy(String txt, File f) throws Exception {
        f.getParentFile().mkdirs();
        FileWriter w = new FileWriter(f);
        w.append(txt);
        w.close();
    }

    final File extractResource(String res, File f) throws Exception {
        URL u = APITest.class.getResource(res);
        assertNotNull ("Resource should be found " + res, u);
        
        FileOutputStream os = new FileOutputStream(f);
        InputStream is = u.openStream();
        for (;;) {
            int ch = is.read ();
            if (ch == -1) {
                break;
            }
            os.write (ch);
        }
        os.close ();
            
        return f;
    }
    
    final static String readFile (java.io.File f) throws java.io.IOException {
        int s = (int)f.length ();
        byte[] data = new byte[s];
        assertEquals ("Read all data", s, new java.io.FileInputStream (f).read (data));
        
        return new String (data);
    }
    
    final File extractString (String res, String nameExt) throws Exception {
        File f = new File(getWorkDir(), nameExt);
        f.deleteOnExit ();
        
        FileOutputStream os = new FileOutputStream(f);
        InputStream is = new ByteArrayInputStream(res.getBytes("UTF-8"));
        for (;;) {
            int ch = is.read ();
            if (ch == -1) {
                break;
            }
            os.write (ch);
        }
        os.close ();
            
        return f;
    }
    
}