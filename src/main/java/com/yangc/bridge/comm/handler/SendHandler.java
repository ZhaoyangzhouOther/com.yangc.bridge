package com.yangc.bridge.comm.handler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.session.IoSession;

import com.google.protobuf.ByteString;
import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.protobuf.ProtobufMessage;
import com.yangc.bridge.comm.protocol.prototype.ProtocolChat;
import com.yangc.bridge.comm.protocol.prototype.ProtocolFile;
import com.yangc.bridge.comm.protocol.prototype.ProtocolResult;

public class SendHandler implements Runnable {

	private SendHandler() {
	}

	public static void sendResult(IoSession session, ResultBean result) throws Exception {
		if (session != null && session.isConnected()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] data = result.getData().getBytes(Server.CHARSET_NAME);

				ProtocolResult protocol = new ProtocolResult();
				protocol.setContentType(ContentType.RESULT);
				protocol.setUuid(result.getUuid().getBytes(Server.CHARSET_NAME));
				protocol.setDataLength(1 + data.length);
				protocol.setSuccess((byte) (result.isSuccess() ? 1 : 0));
				protocol.setData(data);

				session.write(protocol);
			} else {
				ProtobufMessage.Result.Builder builder = ProtobufMessage.Result.newBuilder();
				builder.setUuid(result.getUuid());
				builder.setSuccess(result.isSuccess());
				builder.setData(result.getData());

				session.write(builder.build());
			}
		}
	}

	public static void sendChat(IoSession session, TBridgeChat chat) throws Exception {
		if (session != null && session.isConnected()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = chat.getFrom().getBytes(Server.CHARSET_NAME);
				byte[] to = chat.getTo().getBytes(Server.CHARSET_NAME);
				byte[] data = chat.getData().getBytes(Server.CHARSET_NAME);

				ProtocolChat protocol = new ProtocolChat();
				protocol.setContentType(ContentType.CHAT);
				protocol.setUuid(chat.getUuid().getBytes(Server.CHARSET_NAME));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setDataLength(data.length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setData(data);

				session.write(protocol);
			} else {
				ProtobufMessage.Chat.Builder builder = ProtobufMessage.Chat.newBuilder();
				builder.setUuid(chat.getUuid());
				builder.setFrom(chat.getFrom());
				builder.setTo(chat.getTo());
				builder.setData(chat.getData());

				session.write(builder.build());
			}
		}
	}

	public static void sendReadyFile(IoSession session, TBridgeFile file) throws Exception {
		if (session != null && session.isConnected()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = file.getFrom().getBytes(Server.CHARSET_NAME);
				byte[] to = file.getTo().getBytes(Server.CHARSET_NAME);
				byte[] fileName = file.getFileName().getBytes(Server.CHARSET_NAME);

				ProtocolFile protocol = new ProtocolFile();
				protocol.setContentType(ContentType.READY_FILE);
				protocol.setUuid(file.getUuid().getBytes(Server.CHARSET_NAME));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setFileNameLength((short) fileName.length);
				protocol.setFileName(fileName);
				protocol.setFileSize(file.getFileSize());

				session.write(protocol);
			} else {
				ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
				builder.setUuid(file.getUuid());
				builder.setFrom(file.getFrom());
				builder.setTo(file.getTo());
				builder.setFileName(file.getFileName());
				builder.setFileSize(file.getFileSize());

				session.write(builder.build());
			}
		}
	}

	public static void sendTransmitFile(IoSession session, TBridgeFile file) throws Exception {
		if (session != null && session.isConnected()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = file.getFrom().getBytes(Server.CHARSET_NAME);
				byte[] to = file.getTo().getBytes(Server.CHARSET_NAME);
				byte[] fileName = file.getFileName().getBytes(Server.CHARSET_NAME);

				ProtocolFile protocol = new ProtocolFile();
				protocol.setContentType(ContentType.TRANSMIT_FILE);
				protocol.setUuid(file.getUuid().getBytes(Server.CHARSET_NAME));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setDataLength(fileName.length + 46 + file.getData().length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setFileNameLength((short) fileName.length);
				protocol.setFileName(fileName);
				protocol.setFileSize(file.getFileSize());
				protocol.setFileMd5(file.getFileMd5().getBytes(Server.CHARSET_NAME));
				protocol.setOffset(file.getOffset());
				protocol.setData(file.getData());

				session.write(protocol);
			} else {
				ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
				builder.setUuid(file.getUuid());
				builder.setFrom(file.getFrom());
				builder.setTo(file.getTo());
				builder.setFileName(file.getFileName());
				builder.setFileSize(file.getFileSize());
				builder.setFileMd5(file.getFileMd5());
				builder.setOffset(file.getOffset());
				builder.setData(ByteString.copyFrom(file.getData()));

				session.write(builder.build());
			}
		}
	}

	public static void sendFile(IoSession session, TBridgeFile file) {
		if (file.getFileSize() < 128 * 1024) {
			sendUnreadFile(session, file, 8 * 1024);
		} else {
			new Thread(new SendHandler(session, file, 512 * 1024)).start();
		}
	}

	private static void sendUnreadFile(IoSession session, TBridgeFile file, int bufferedLength) {
		if (session != null && session.isConnected()) {
			File unreadFile = new File(FileUtils.getTempDirectoryPath() + "/com.yangc.bridge/" + file.getTo() + "/" + file.getUuid());
			BufferedInputStream bis = null;
			try {
				int offset = -1;
				byte[] data = new byte[bufferedLength];
				if (StringUtils.equals(Server.CODEC, "prototype")) {
					byte[] from = file.getFrom().getBytes(Server.CHARSET_NAME);
					byte[] to = file.getTo().getBytes(Server.CHARSET_NAME);
					byte[] fileName = file.getFileName().getBytes(Server.CHARSET_NAME);

					ProtocolFile protocol = new ProtocolFile();
					protocol.setContentType(ContentType.TRANSMIT_FILE);
					protocol.setUuid(file.getUuid().getBytes(Server.CHARSET_NAME));
					protocol.setFromLength((short) from.length);
					protocol.setToLength((short) to.length);
					protocol.setDataLength(fileName.length + 46 + data.length);
					protocol.setFrom(from);
					protocol.setTo(to);
					protocol.setFileNameLength((short) fileName.length);
					protocol.setFileName(fileName);
					protocol.setFileSize(file.getFileSize());
					protocol.setFileMd5(file.getFileMd5().getBytes(Server.CHARSET_NAME));

					bis = new BufferedInputStream(new FileInputStream(unreadFile));
					while ((offset = bis.read(data)) != -1) {
						protocol.setOffset(offset);
						protocol.setData(data);
						session.write(protocol);
					}
				} else {
					ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
					builder.setUuid(file.getUuid());
					builder.setFrom(file.getFrom());
					builder.setTo(file.getTo());
					builder.setFileName(file.getFileName());
					builder.setFileSize(file.getFileSize());
					builder.setFileMd5(file.getFileMd5());

					bis = new BufferedInputStream(new FileInputStream(unreadFile));
					while ((offset = bis.read(data)) != -1) {
						builder.setOffset(offset);
						builder.setData(ByteString.copyFrom(data));
						session.write(builder.build());
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (bis != null) bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private IoSession session;
	private TBridgeFile file;
	private int bufferedLength;

	private SendHandler(IoSession session, TBridgeFile file, int bufferedLength) {
		this.session = session;
		this.file = file;
		this.bufferedLength = bufferedLength;
	}

	@Override
	public void run() {
		sendUnreadFile(session, file, bufferedLength);
	}

}
