package fr.upem.jarret.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class HTTPReader {

	private final SocketChannel sc;
	private final ByteBuffer buff;

	public HTTPReader(SocketChannel sc, ByteBuffer buff) {
		this.sc = sc;
		this.buff = buff;
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 *         <p>
	 *         The method assume that buff is in write mode and leave it in
	 *         write-mode The method never reads from the socket as long as the
	 *         buffer is not empty
	 * @throws IOException
	 *             HTTPException if the connection is closed before a line could
	 *             be read
	 */
	public String readLineCRLF() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean lastCR = false;
		boolean finished = false;
		while(true) {
			buff.flip();
			while(buff.hasRemaining() && !finished) {
				byte current = buff.get();
				sb.append((char)current);
				if(current == '\n' && lastCR) {
					finished = true;
				}
				lastCR = current == '\r';
			}
			buff.compact();
			if(finished)
				break;
			if(sc.read(buff)==-1) {
				throw new HTTPException();
			}
		}

		sb.delete(sb.length()-2, sb.length());

		return sb.toString();
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException
	 *             HTTPException if the connection is closed before a header
	 *             could be read if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		HashMap<String,String> map = new HashMap<String,String>(); 

		String firstline = readLineCRLF();
		String string;

		while(!(string = readLineCRLF()).equals("")) {
			String pvirg=";";
			String[] values = string.split(": ");    	
			if(map.containsKey(values[0])) {
				pvirg.concat(values[1]);
				map.put(values[0], pvirg);
			}
			else
				map.put(values[0], values[1]);

		}

		return HTTPHeader.create(firstline, map);
	}

	private static boolean ReadFully(ByteBuffer bb,SocketChannel sc ) throws IOException{
		while(bb.hasRemaining()) {
			int n = sc.read(bb);
			if(n == -1){
				return false;
			}
		}

		return true;
	}
	/**
	 * @return The HTTPHeaderServer object corresponding to the header read 
	 * @throws IOException
	 *         HTTPException if the connection is closed before a header
	 *         could be read if the header is ill-formed
	 */
	public HTTPHeader readHeaderServer() throws IOException {
		HashMap<String,String> map = new HashMap<String,String>(); 
		
		String firstline = readLineCRLF();
		String string;

		while(!(string = readLineCRLF()).equals("")) {
			String pvirg=";";
			String[] values = string.split(": ");    	

			if(map.containsKey(values[0])) {
				pvirg.concat(values[1]);
				map.put(values[0], pvirg);
			}
			else
				map.put(values[0], values[1]);
		}

		return HTTPHeader.createHeaderServer(firstline, map);
	}




	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the
	 *         socket
	 * @throws IOException
	 *             HTTPException is the connection is closed before all bytes
	 *             could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {

		ByteBuffer buffContent = ByteBuffer.allocate(size);
		buff.flip();
		if(buff.remaining() > size) {
			int oldLimit = buff.limit();
			buff.limit(size);
			buffContent.put(buff);
			buff.limit(oldLimit);
			buff.compact();
			return buffContent;
		}
		buffContent.put(buff);
		buff.compact();

		if(!ReadFully(buffContent,sc)) {
			System.out.println("server closed connection");
		}

		return buffContent; 
	}

}