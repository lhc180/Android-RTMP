/*
 * Camundo <http://www.camundo.com> Copyright (C) 2011  Wouter Van der Beken.
 *
 * This file is part of Camundo.
 *
 * Camundo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Camundo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Camundo.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.camundo.media.pipe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.util.Log;

public class FFMPEGAudioInputPipe extends Thread implements AudioInputPipe {
	
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static final String TAG = "FFMPEGAudioInputPipe";
	
	private static final String IO_ERROR = "I/O error";
	
	private Process process;
	private String command;
	private boolean processRunning;
	protected boolean processStartFailed;
	
	private InputstreamReaderThread errorStreamReaderThread;
//	private InputstreamReaderThread inputStreamReaderThread;
	
	private OutputStream outputStream;
	
	private byte[] bootstrap;
	
	
	public FFMPEGAudioInputPipe( String command ) {
		this.command = command;
	}
	
	
	public void write( int oneByte ) throws IOException {
		outputStream.write(oneByte);
	}
	
	
	
	public void write( byte[] buffer, int offset, int length ) throws IOException {
		outputStream.write(buffer, offset, length);
		outputStream.flush();
	}
	
	
	public void setBootstrap( byte[] bootstrap ) {
		this.bootstrap = bootstrap;
	}
	
	public void writeBootstrap() {
		if ( bootstrap != null ){
			try {
				write( bootstrap, 0, bootstrap.length);
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void close() {
		
		if ( process != null ) {
			Log.i( TAG , "[ close() ] destroying process");
			process.destroy();
			process = null;
		}
		else {
			Log.i( TAG , "[ close() ] can not destroy process -> is null");
		}
		
		Log.i( TAG , "[ close() ] closing outputstream");
		try {
			synchronized (outputStream) {
				outputStream.close();
				Log.i( TAG , "[ close() ] closing outputstream done");
			}
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		//inputStreamReaderThread.finish();
		try {
			errorStreamReaderThread.finish();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void run () {
        try {
            process = Runtime.getRuntime().exec(  command , null);
            
//            inputStreamReaderThread = new InputstreamReaderThread(process.getInputStream());
//            Log.d("FFMPEGInputPipe", "[ run() ] inputStreamReader created");
            errorStreamReaderThread = new InputstreamReaderThread(process.getErrorStream());
//            Log.d("FFMPEGInputPipe", "[ run() ] errorStreamReader created");
//             
//            inputStreamReaderThread.start();
            errorStreamReaderThread.start();
            outputStream = process.getOutputStream();
            
            if ( outputStream != null) {
            	processRunning = true;
            }
            else {
            	processStartFailed = true;
            }
            try {
            	process.waitFor();
            }
            catch( Exception e ) {
            	e.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
   }
	
	
	@Override
	public boolean initialized() throws IOException {
		if ( processStartFailed ) {
			throw new IOException("Process start failed");
		}
		return processRunning;
	}
	

	
	class InputstreamReaderThread extends Thread {

		private InputStream inputStream;
		
		public InputstreamReaderThread( InputStream i ){
			this.inputStream = i;
		}
		
		@Override
		public void run() {
            try {
                 InputStreamReader isr = new InputStreamReader(inputStream);
                 BufferedReader br = new BufferedReader(isr, 32);
                 String line;
                 while ((line = br.readLine()) != null) {
                	 if ( line.indexOf(IO_ERROR) != -1 ) {
                		 Log.e( TAG , "IOERRRRRORRRRR -> putting to processStartFailed");
                		 processStartFailed = true;
                	 }
                	 //Log.d( TAG , line + LINE_SEPARATOR);
                 }
            }
            catch( Exception e ) {
                 e.printStackTrace();
            }
       }
		
		public void finish() {
			if ( inputStream != null ) {
				try {
					inputStream.close();
				}
				catch( Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}


}
