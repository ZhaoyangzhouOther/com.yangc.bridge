package com.yangc.bridge.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ClientStatus;
import com.yangc.bridge.bean.ServerStatus;
import com.yangc.bridge.comm.cache.SessionCache;
import com.yangc.bridge.comm.factory.DataCodecFactory;
import com.yangc.bridge.comm.handler.ServerHandler;
import com.yangc.utils.Message;

@Service("com.yangc.bridge.comm.Server")
public class Server {

	private static final Logger logger = Logger.getLogger(Server.class);

	private static final String IP = Message.getMessage("bridge.ipAddress");
	private static final int PORT = Integer.parseInt(Message.getMessage("bridge.port"));
	private static final int TIMEOUT = Integer.parseInt(Message.getMessage("bridge.timeout"));

	@Autowired
	private ServerHandler serverHandler;

	private NioSocketAcceptor acceptor;

	private void init() {
		this.acceptor = new NioSocketAcceptor();
		// 设置空闲时间
		this.acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, TIMEOUT);
		// 设置过滤器
		DefaultIoFilterChainBuilder filterChain = this.acceptor.getFilterChain();
		filterChain.addLast("codec", new ProtocolCodecFilter(new DataCodecFactory()));
		this.acceptor.setHandler(this.serverHandler);
		try {
			this.acceptor.bind(new InetSocketAddress(IP, PORT));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		logger.info("==========mina服务启动=========");
		this.init();
	}

	public void restart() {
		logger.info("==========重启mina服务=========");
		if (this.acceptor != null) {
			this.acceptor.dispose();
			this.acceptor = null;
		}
		this.init();
	}

	public boolean isActive() {
		if (this.acceptor != null) {
			return this.acceptor.isActive();
		}
		return false;
	}

	public ServerStatus getServerStatus() {
		ServerStatus serverStatus = new ServerStatus();
		serverStatus.setIpAddress(IP);
		serverStatus.setPort(PORT);
		serverStatus.setTimeout(TIMEOUT);
		serverStatus.setActive(this.isActive());
		return serverStatus;
	}

	public List<ClientStatus> getClientStatusList() {
		Map<String, Long> map = SessionCache.getSessionCache();
		Map<Long, IoSession> managedSessions = this.acceptor.getManagedSessions();

		List<ClientStatus> clientStatusList = new ArrayList<ClientStatus>(map.size());
		for (Entry<String, Long> entry : map.entrySet()) {
			IoSession session = managedSessions.get(entry.getValue());
			if (session != null) {
				ClientStatus clientStatus = new ClientStatus();
				clientStatus.setUsername(entry.getKey());
				clientStatus.setIpAddress(((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress());
				clientStatus.setSessionId(entry.getValue());
				clientStatus.setLastIoTime(DateFormatUtils.format(session.getLastIoTime(), "yyyy-MM-dd HH:mm:ss"));
				clientStatusList.add(clientStatus);
			}
		}
		return clientStatusList;
	}

}
