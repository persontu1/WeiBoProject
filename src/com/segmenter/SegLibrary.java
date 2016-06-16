package com.segmenter;

import com.sun.jna.Library;

public interface SegLibrary extends Library {

	public int NLPIR_Init(byte[] sDataPath, int encoding, byte[] sLicenceCode);

	public String NLPIR_ParagraphProcess(String sSrc, int bPOSTagged);

	public String NLPIR_GetKeyWords(String sLine, int nMaxKeyLimit, boolean bWeightOut);

	public void NLPIR_Exit();

	public String NLPIR_GetLastErrorMsg();
}
