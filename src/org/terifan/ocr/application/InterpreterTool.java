package org.terifan.ocr.application;

import java.awt.Rectangle;


interface InterpreterTool
{
    public void performAction(Rectangle aRectangle);

	public void onReleaseFocus();
}
