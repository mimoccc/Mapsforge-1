package org.mapsforge.core.graphics;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Stack;

import org.mapsforge.core.PorterDuffXfermode;
import org.mapsforge.core.PorterDuffXfermode.Mode;
import org.mapsforge.core.Xfermode;
import org.mapsforge.core.graphics.Paint.Align;
import org.mapsforge.core.graphics.Paint.FontInfo;
import org.mapsforge.core.graphics.Paint.Style;

public class Canvas {
	
	private BufferedImage mBufferedImage;
    private final Stack<Graphics2D> mGraphicsStack = new Stack<Graphics2D>();
	
    public Canvas() {}
    
    public Canvas(Bitmap bitmap) {
    	mBufferedImage = bitmap.getImage();
        mGraphicsStack.push(mBufferedImage.createGraphics());
	}

	public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
		boolean needsRestore = false;
        if (matrix.isIdentity() == false) {
            // create a new graphics and apply the matrix to it
            save(); // this creates a new Graphics2D, and stores it for children call to use
            needsRestore = true;
            Graphics2D g = getGraphics2d(); // get the newly create Graphics2D

            // get the Graphics2D current matrix
            AffineTransform currentTx = g.getTransform();
            // get the AffineTransform from the matrix
            AffineTransform matrixTx = matrix.getTransform();

            // combine them so that the matrix is applied after.
            currentTx.preConcatenate(matrixTx);

            // give it to the graphics as a new matrix replacing all previous transform
            g.setTransform(currentTx);
        }

        // draw the bitmap
        drawBitmap(bitmap, 0, 0, paint);

        if (needsRestore) {
            // remove the new graphics
            restore();
        }
	}

	private void restore() {
        mGraphicsStack.pop();		
	}

	private int save() {
		 // get the current save count
        int count = mGraphicsStack.size();

        // create a new graphics and add it to the stack
        Graphics2D g = (Graphics2D)getGraphics2d().create();
        mGraphicsStack.push(g);

        // return the old save count
        return count;
	}

	public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
		drawBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                (int)left, (int)top,
                (int)left+bitmap.getWidth(), (int)top+bitmap.getHeight(), paint);
	}


	private void drawBitmap(Bitmap bitmap, int sleft, int stop, int sright, int sbottom, int dleft,
            int dtop, int dright, int dbottom, Paint paint) {
        BufferedImage image = bitmap.getImage();

        Graphics2D g = getGraphics2d();

        Composite c = null;

        if (paint != null) {
            if (paint.isFilterBitmap()) {
                g = (Graphics2D)g.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }

            if (paint.getAlpha() != 0xFF) {
                c = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        paint.getAlpha()/255.f));
            }
        }

        g.drawImage(image, dleft, dtop, dright, dbottom,
                sleft, stop, sright, sbottom, null);

        if (paint != null) {
            if (paint.isFilterBitmap()) {
                g.dispose();
            }
            if (c != null) {
                g.setComposite(c);
            }
        }
    }

	public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);

        g.drawLine((int)startX, (int)startY, (int)stopX, (int)stopY);

        // dispose Graphics2D object
        g.dispose();
    }
	
	private Graphics2D getCustomGraphics(Paint paint) {
		// make new one
        Graphics2D g = getGraphics2d();
        g = (Graphics2D)g.create();

        // configure it
        g.setColor(new java.awt.Color(paint.getColor()));
        int alpha = paint.getAlpha();
        float falpha = alpha / 255.f;

        Style style = paint.getStyle();
        if (style == Style.STROKE || style == Style.FILL_AND_STROKE) {
            PathEffect e = paint.getPathEffect();
            if (e instanceof DashPathEffect) {
                DashPathEffect dpe = (DashPathEffect)e;
                g.setStroke(new BasicStroke(
                        paint.getStrokeWidth(),
                        paint.getStrokeCap().getJavaCap(),
                        paint.getStrokeJoin().getJavaJoin(),
                        paint.getStrokeMiter(),
                        dpe.getIntervals(),
                        dpe.getPhase()));
            } else {
                g.setStroke(new BasicStroke(
                        paint.getStrokeWidth(),
                        paint.getStrokeCap().getJavaCap(),
                        paint.getStrokeJoin().getJavaJoin(),
                        paint.getStrokeMiter()));
            }
        }

        Xfermode xfermode = paint.getXfermode();
        if (xfermode instanceof PorterDuffXfermode) {
            Mode mode = ((PorterDuffXfermode)xfermode).getMode();

            setModeInGraphics(mode, g, falpha);
        } else {
        	//TODO?
            /*if (mLogger != null && xfermode != null) {
                mLogger.warning(String.format(
                        "Xfermode '%1$s' is not supported in the Layout Editor.",
                        xfermode.getClass().getCanonicalName()));
            }*/
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));
        }

        Shader shader = paint.getShader();
        if (shader != null) {
            java.awt.Paint shaderPaint = shader.getJavaPaint();
            if (shaderPaint != null) {
                g.setPaint(shaderPaint);
            } else {
            	//TODO?
                /*if (mLogger != null) {
                    mLogger.warning(String.format(
                            "Shader '%1$s' is not supported in the Layout Editor.",
                            shader.getClass().getCanonicalName()));
                }*/
            }
        }

        return g;
	}

	private void setModeInGraphics(Mode mode, Graphics2D g, float falpha) {
		switch (mode) {
        case CLEAR:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, falpha));
            break;
        case DARKEN:
            break;
        case DST:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST, falpha));
            break;
        case DST_ATOP:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_ATOP, falpha));
            break;
        case DST_IN:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN, falpha));
            break;
        case DST_OUT:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, falpha));
            break;
        case DST_OVER:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, falpha));
            break;
        case LIGHTEN:
            break;
        case MULTIPLY:
            break;
        case SCREEN:
            break;
        case SRC:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, falpha));
            break;
        case SRC_ATOP:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, falpha));
            break;
        case SRC_IN:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, falpha));
            break;
        case SRC_OUT:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT, falpha));
            break;
        case SRC_OVER:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));
            break;
        case XOR:
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR, falpha));
            break;
    }
	}

	public void drawLines(float[] pts, Paint paint) {
        drawLines(pts, 0, pts.length, paint);
    }
	
	public void drawLines(float[] pts, int offset, int count, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);

        for (int i = 0 ; i < count ; i += 4) {
            g.drawLine((int)pts[i + offset], (int)pts[i + offset + 1],
                    (int)pts[i + offset + 2], (int)pts[i + offset + 3]);
        }

        // dispose Graphics2D object
        g.dispose();
    }

	public void drawPath(Path path, Paint paint) {
		// get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);

        Style style = paint.getStyle();

        // draw
        if (style == Style.FILL || style == Style.FILL_AND_STROKE) {
            g.fill(path.getAwtShape());
        }

        if (style == Style.STROKE || style == Style.FILL_AND_STROKE) {
            g.draw(path.getAwtShape());
        }

        // dispose Graphics2D object
        g.dispose();
	}
	
	public void drawText(String text, float x, float y, Paint paint) {
		drawText(text.toCharArray(), 0, text.length(), x, y, paint);
	}

	public void drawText(char[] text, int index, int count, float x, float y, Paint paint) {
        // WARNING: the logic in this method is similar to Paint.measureText.
        // Any change to this method should be reflected in Paint.measureText
        Graphics2D g = getGraphics2d();

        g = (Graphics2D)g.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // set the color. because this only handles RGB, the alpha channel is handled
        // as a composite.
        g.setColor(new java.awt.Color(paint.getColor()));
        int alpha = paint.getAlpha();
        float falpha = alpha / 255.f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));


        // Paint.TextAlign indicates how the text is positioned relative to X.
        // LEFT is the default and there's nothing to do.
        if (paint.getTextAlign() != Align.LEFT) {
            float m = paint.measureText(text, index, count);
            if (paint.getTextAlign() == Align.CENTER) {
                x -= m / 2;
            } else if (paint.getTextAlign() == Align.RIGHT) {
                x -= m;
            }
        }

        List<FontInfo> fonts = paint.getFonts();
        try {
            if (fonts.size() > 0) {
                FontInfo mainFont = fonts.get(0);
                int i = index;
                int lastIndex = index + count;
                while (i < lastIndex) {
                    // always start with the main font.
                    int upTo = mainFont.mFont.canDisplayUpTo(text, i, lastIndex);
                    if (upTo == -1) {
                        // draw all the rest and exit.
                        g.setFont(mainFont.mFont);
                        g.drawChars(text, i, lastIndex - i, (int)x, (int)y);
                        return;
                    } else if (upTo > 0) {
                        // draw what's possible
                        g.setFont(mainFont.mFont);
                        g.drawChars(text, i, upTo - i, (int)x, (int)y);

                        // compute the width that was drawn to increase x
                        x += mainFont.mMetrics.charsWidth(text, i, upTo - i);

                        // move index to the first non displayed char.
                        i = upTo;

                        // don't call continue at this point. Since it is certain the main font
                        // cannot display the font a index upTo (now ==i), we move on to the
                        // fallback fonts directly.
                    }

                    // no char supported, attempt to read the next char(s) with the
                    // fallback font. In this case we only test the first character
                    // and then go back to test with the main font.
                    // Special test for 2-char characters.
                    boolean foundFont = false;
                    for (int f = 1 ; f < fonts.size() ; f++) {
                        FontInfo fontInfo = fonts.get(f);

                        // need to check that the font can display the character. We test
                        // differently if the char is a high surrogate.
                        int charCount = Character.isHighSurrogate(text[i]) ? 2 : 1;
                        upTo = fontInfo.mFont.canDisplayUpTo(text, i, i + charCount);
                        if (upTo == -1) {
                            // draw that char
                            g.setFont(fontInfo.mFont);
                            g.drawChars(text, i, charCount, (int)x, (int)y);

                            // update x
                            x += fontInfo.mMetrics.charsWidth(text, i, charCount);

                            // update the index in the text, and move on
                            i += charCount;
                            foundFont = true;
                            break;

                        }
                    }

                    // in case no font can display the char, display it with the main font.
                    // (it'll put a square probably)
                    if (foundFont == false) {
                        int charCount = Character.isHighSurrogate(text[i]) ? 2 : 1;

                        g.setFont(mainFont.mFont);
                        g.drawChars(text, i, charCount, (int)x, (int)y);

                        // measure it to advance x
                        x += mainFont.mMetrics.charsWidth(text, i, charCount);

                        // and move to the next chars.
                        i += charCount;
                    }
                }
            }
        } finally {
            g.dispose();
        }	
	}

	public void drawTextOnPath(String text, Path path, int i, int j, Paint paint) {
		// TODO Auto-generated method stub
	}
	
	/* GETTERS AND SETTERS */
	public void setBitmap(Bitmap bitmap) {
		mBufferedImage = bitmap.getImage();
        mGraphicsStack.push(mBufferedImage.createGraphics());	
	}
	
	public int getWidth() {
		return mBufferedImage.getWidth();
	}

	public int getHeight() {
		return mBufferedImage.getHeight();
	}
	
	private Graphics2D getGraphics2d() {
		return mGraphicsStack.peek();
	}
}
