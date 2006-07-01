package org.hyperic.sigar.test;

import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.SigarNotImplementedException;

import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.sigar.ptql.MalformedQueryException;
import org.hyperic.sigar.ptql.QueryLoadException;

public class TestPTQL extends SigarTestCase {

    private static final String[] OK_QUERIES = {
        "State.Name.eq=java", //all java processs
        "Exe.Name.ew=java",   //similar
        "State.Name.eq=java,Exe.Cwd.eq=$user.dir", //process running this test
        "State.Name.ne=java,Exe.Cwd.eq=$user.dir", //parent(s) of process running this test
        "State.Name.sw=httpsd,State.Name.Pne=$1", //httpsd parent process
        "State.Name.ct=ssh", //anything ssh, "ssh", "ssh-agent", "sshd"
        //disabled to pass with jdk 1.3
        //"Args.-1.eq=weblogic.Server,Env.WEBLOGIC_CLASSPATH.re=.*weblogic.jar.*", //weblogic
        "State.Name.eq=java,Args.-1.ew=AgentClient", //hq agents
        "Cred.Uid.eq=1003,State.Name.eq=java,Args.-1.ew=AgentClient", //my hq agent
        "Cred.Uid.gt=0,Cred.Uid.lt=1000", //range of users
        "Cred.Uid.eq=1003,Cred.Gid.eq=1003", //me
        "CredName.User.eq=dougm", //me
        "Time.Sys.gt=1000", //cpu hog
        "Fd.Total.gt=20", //lots of open files
        "Mem.Size.ge=10000000,Mem.Share.le=1000000", //memory hog
        "State.Name.eq=sshd,Cred.Uid.eq=0",
        "State.Name.eq=crond,Cred.Uid.eq=0",
        "State.State.eq=R", //processes in read state
        "Args.0.eq=sendmail: accepting connections",
        "Args.0.sw=sendmail: Queue runner@",
        "Args.1000.eq=foo",
        "Args.*.eq=org.apache.tools.ant.Main", //'*' == any arg
        "Args.*.ct=java", //'*' == any arg
        "Args.*.ew=sigar.jar", //'*' == any arg
        "Port.tcp.eq=80,Cred.Uid.eq=0", //root owned http port
        "Port.udp.eq=161,Cred.Uid.eq=0", //root owned snmp port
        "Port.tcp.eq=8080,Cred.Uid.eq=1003", //dougm owned jboss port
        "Pid.PidFile.eq=pid.file",
        "Pid.Pid.eq=1",
        "Pid.Service.eq=Eventlog",
        "Pid.Service.eq=NOSUCHSERVICE",
        "Pid.Service.eq=Hyperic HQ Agent",
        "State.Name.eq=java,Pid.Pid.ne=$$", //all java procs cept this one
        "Cpu.Percent.ge=0.2",
    };

    private static final String[] MALFORMED_QUERIES = {
        "foo",
        "State.Name",
        "State.Name.eq",
        "State.Namex.eq=foo",
        "Statex.Name.eq=foo",
        "State.Name.eqx=foo",
        "State.Name.Xeq=foo",
        "State.Name.eq=$2",
        "State.State.eq=read",
        "Args.x.eq=foo",
        "Time.Sys.gt=x",
        "Pid.PidFile.ne=pid.file",
        "Pid.Pid.eq=foo",
        "Pid.Service.ne=Eventlog",
        "Cpu.Percent.ge=x",
        "Port.foo.eq=8080",
        "",
        null
    };

    private static final String[] LOAD_FAIL_QUERIES = {
    };

    public TestPTQL(String name) {
        super(name);
    }

    private int runQuery(SigarProxy proxy, String qs)
        throws MalformedQueryException,
               QueryLoadException,
               SigarException {

        ProcessQuery query =
            ProcessQueryFactory.getInstance(qs);

        ProcessFinder finder = new ProcessFinder(proxy);

        try {
            long[] pids = finder.find(query);

            traceln(pids.length + " processes match: " + qs);

            if (pids.length == 1) {
                long pid = finder.findSingleProcess(query);
                assertTrue(pid + "==" + pids[0],
                           pid == pids[0]);
            }
            return pids.length;
        } catch (SigarNotImplementedException e) {
            return 0;
        }
    }

    private void testOK(SigarProxy proxy) throws Exception {
        for (int i=0; i<OK_QUERIES.length; i++) {
            String qs = OK_QUERIES[i];
            assertTrue(qs,
                       runQuery(proxy, qs) >= 0);
        }
    }

    private void testMalformed(SigarProxy proxy) throws Exception {
        for (int i=0; i<MALFORMED_QUERIES.length; i++) {
            String qs = MALFORMED_QUERIES[i];
            try {
                runQuery(proxy, qs);
                fail("'" + qs + "' did not throw MalformedQueryException");
            } catch (MalformedQueryException e) {
                traceln(qs + ": " + e.getMessage());
                assertTrue(qs + " Malformed", true);
            }
        }
    }

    private void testLoadFailure(SigarProxy proxy) throws Exception {
        for (int i=0; i<LOAD_FAIL_QUERIES.length; i++) {
            String qs = LOAD_FAIL_QUERIES[i];
            try {
                runQuery(proxy, qs);
                fail(qs + " did not throw QueryLoadException");
            } catch (QueryLoadException e) {
                traceln(qs + ": " + e.getMessage());
                assertTrue(qs + " QueryLoad", true);
            }
        }
    }

    public void testCreate() throws Exception {
        SigarProxy proxy =
            SigarProxyCache.newInstance(getSigar());

        testOK(proxy);
        testMalformed(proxy);
        testLoadFailure(proxy);
    }
}
