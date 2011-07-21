package org.mapsforge.core.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.mapsforge.core.graphics.Paint.Align;
import org.mapsforge.core.graphics.Paint.Style;

public class Canvas {

	public BufferedImage mBufferedImage;
	
	private static final long serialVersionUID = 5085355825188623626L;
	private Properties propertiesSettings;
	
    public Canvas() {
    	//Properties
		try{
			this.propertiesSettings = new Properties();
			this.propertiesSettings.load(new FileReader("res/config/config.properties"));
		} catch (IOException e) {
			//JOptionPane.showMessageDialog(this, "Could not read properties files!");
		}
		int width = Integer.parseInt(propertiesSettings.getProperty("map_size_width"));
		int height = Integer.parseInt(propertiesSettings.getProperty("map_size_width"));
    	mBufferedImage = new BufferedImage(width , height, BufferedImage.TYPE_INT_RGB);
    }

	public Canvas(Bitmap bitmap) {
		mBufferedImage = bitmap.getImage();
	}


	//TODO
	public void drawText(String text, float x, float y, Paint paint) {
		Graphics2D g = mBufferedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(paint.getColor().getRGB()));
		int alpha = paint.getAlpha();
		float falpha = alpha / 255.f;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));
		if(paint.getTextAlign() == Align.LEFT) {
			float m = paint.measureText(text.toCharArray(), 0, text.length());
			if(paint.getTextAlign() == Align.CENTER) {
				x -= m / 2;
			} else if(paint.getTextAlign() == Align.RIGHT) {
				x -= m;
			}
		}
		//TODO Changed
		int i = 0;
		int lastIndex = 0 + text.length();
		Typeface font = paint.getTypeface();
		FontMetrics mMetrics = g.getFontMetrics(font);
		while(i < lastIndex) {
			int upTo = font.canDisplayUpTo(text.toCharArray(), i, lastIndex);
			if(upTo == -1) {
				g.setFont(font);

				g.drawChars(text.toCharArray(), i, lastIndex - i, (int) x, (int) y);
				return;
			} else if(upTo > 0) {
				g.setFont(font);
				g.drawChars(text.toCharArray(), i, upTo - i, (int) x, (int) y);
				x += mMetrics.charsWidth(text.toCharArray(), i, upTo - i);
				i = upTo;
			}
			boolean foundFont = false;
	
			if(!foundFont) {
				int charCount = Character.isHighSurrogate(text.toCharArray()[i]) ? 2 : 1;
				g.setFont(font);
				g.drawChars(text.toCharArray(), i, charCount, (int) x, (int) y);
				x += mMetrics.charsWidth(text.toCharArray(), i, charCount);
				i += charCount;
			}
		}				
		g.dispose();
	}
	
	/* (non-Javadoc)
     * @see android.graphics.Canvas#drawBitmap(android.graphics.Bitmap, android.graphics.Matrix, android.graphics.Paint)
     */
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        if (matrix.isIdentity() == false) {
            // create a new graphics and apply the matrix to it
            //save(); // this creates a new Graphics2D, and stores it for children call to use
            //needsRestore = true;
            Graphics2D g = mBufferedImage.createGraphics(); // get the newly create Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
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

        //if (needsRestore) {
            // remove the new graphics
            //restore();
        //}
    }

    /* (non-Javadoc)
     * @see android.graphics.Canvas#drawTextOnPath(text, path, int, int, android.graphics.Paint)
     */
	public void drawTextOnPath(String text, Path path, int i, int j, Paint paint) {
		Graphics2D g = mBufferedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Style style = paint.getStyle();

		//TODO TextStroke t = new TextStroke(text, new Font("Serif", Font.PLAIN, 10));
		//Dummy Font
		TextStroke t = new TextStroke(text, new Font("Serif", Font.PLAIN, 8));
		GeneralPath generalPath = (GeneralPath) t.createStrokedShape(path.getAwtShape());
		
		Path newPath = new Path(generalPath);
		// draw
        if (style == Style.FILL || style == Style.FILL_AND_STROKE) {
            g.fill(newPath.getAwtShape());
        }

        if (style == Style.STROKE || style == Style.FILL_AND_STROKE) {
            g.draw(newPath.getAwtShape());
        }

        // dispose Graphics2D object
        g.dispose();
	}

	/* (non-Javadoc)
     * @see android.graphics.Canvas#drawLines(float[], android.graphics.Paint)
     */
	public void drawLines(float[] pts, Paint paint) {
		Graphics2D g = mBufferedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		for(int i = 0; i < pts.length; i += 4) {
			g.drawLine((int) pts[i + 0], (int) pts[i + 0 + 1], (int) pts[i + 0 + 2], (int) pts[i + 0 + 3]);
		}
		g.dispose();		
	}

	/* (non-Javadoc)
     * @see android.graphics.Canvas#drawPath(android.graphics.Path, android.graphics.Paint)
     */
    public void drawPath(Path path, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
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

    /* (non-Javadoc)
     * @see android.graphics.Canvas#drawLine(float, float, float, float, android.graphics.Paint)
     */
    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        // get a Graphics2D object configured with the drawing parameters.
        Graphics2D g = getCustomGraphics(paint);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawLine((int)startX, (int)startY, (int)stopX, (int)stopY);

        // dispose Graphics2D object
        g.dispose();
    }
    
    /* (non-Javadoc)
     * @see android.graphics.Canvas#drawBitmap(float, float, float, float, android.graphics.Paint)
     */
	public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
		BufferedImage image = bitmap.getImage();

        Graphics2D g = mBufferedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
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

        g.drawImage(image, (int) left, (int) top, (int) left+bitmap.getWidth(), (int) top+bitmap.getHeight(),
                0, 0, bitmap.getWidth(), bitmap.getHeight(), null);
        
        if (paint != null) {
            if (paint.isFilterBitmap()) {
                g.dispose();
            }
            if (c != null) {
                g.setComposite(c);
            }
        }		
	}

	public void setBitmap(Bitmap bitmap) {
		mBufferedImage = bitmap.getImage();
	}
    
	/**
     * Creates a new {@link Graphics2D} based on the {@link Paint} parameters.
     * <p/>The object must be disposed ({@link Graphics2D#dispose()}) after being used.
     */
    private Graphics2D getCustomGraphics(Paint paint) {
        // make new one
        Graphics2D g = mBufferedImage.createGraphics();
        g = (Graphics2D)g.create();

        // configure it
        g.setColor(new Color(paint.getColor().getRGB()));
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

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, falpha));

        Shader shader = paint.getShader();
        if (shader != null) {
            java.awt.Paint shaderPaint = shader.getJavaPaint();
            if (shaderPaint != null) {
                g.setPaint(shaderPaint);
            }
        }

        return g;
    }

	public int getWidth() {
		return mBufferedImage.getWidth();
	}

	public int getHeight() {
		return mBufferedImage.getHeight();
	}
	
}
