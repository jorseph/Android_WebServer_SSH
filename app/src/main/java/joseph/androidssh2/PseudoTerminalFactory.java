package joseph.androidssh2;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.InvertedShellWrapper;

public class PseudoTerminalFactory implements Factory<Command> {

	private String cmd;
	private String[] args;

	public PseudoTerminalFactory(String cmd, String... args) {
		super();
		this.cmd = cmd;
		this.args = args;
	}

	@Override
	public Command create() {
		return new InvertedShellWrapper(new PseudoTerminal());
	}

	private class PseudoTerminal implements InvertedShell {

		private OutputStream stdin;
		private InputStream stdout;
		private InputStream stderr;
		private FileDescriptor fd;
		private int pid;
		private AtomicBoolean destroyed = new AtomicBoolean(false);

		@Override
		public void destroy() {
			android.os.Process.killProcess(pid);
			destroyed.set(true);
		}

		@Override
		public int exitValue() {
			return Exec.waitFor(pid);
		}

		@Override
		public InputStream getErrorStream() {
			return stderr;
		}

		@Override
		public OutputStream getInputStream() {
			return stdin;
		}

		@Override
		public InputStream getOutputStream() {
			return stdout;
		}

		@Override
		public boolean isAlive() {
			return !destroyed.get();
		}

		@Override
		public void start(Map<String, String> env) throws IOException {
			int[] pidOut = new int[1];
			fd = Exec.createSubprocess(cmd, args[0], null, pidOut);
			pid = pidOut[0];

			stdin = new FileOutputStream(fd);
			stdout = new FileInputStream(fd);
			stderr = new FileInputStream("/dev/null");
		}
	}

}
