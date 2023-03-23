package com.example.futbolitomakakos

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


//x
private var velocidadX = 0.0f
private var posX = 0.0f
private var xMax = 0.0f
private var acelerometroX = 0.0f

//y
private var velocidadY = 0.0f
private var posY = 0.0f
private var yMax = 0.0f
private var acelerometroY = 0.0f

private var porteriaInicio = 0.0f
private var porteriaFinal = 0.0f
private var frameTime = 0.666f

private val anchoPelota = 130
private val anchoPorteria = 330
private val alturaPelota = 130
private val alturaPorteria = 300

private var scoreA = 0
private var scoreB = 0
private var esGol = false
lateinit var pelota : Bitmap
lateinit var porteriaA : Bitmap
lateinit var porteriaB : Bitmap
lateinit var cancha : Bitmap



class MainActivity : AppCompatActivity(),SensorEventListener {


    private lateinit var sensorManager: SensorManager
    private  var sensorACCELEROMETER: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        //Tomamos los datos de la pantalla
        var tamanio = Point()
        var display :Display = windowManager.defaultDisplay
        display.getSize(tamanio)

        val miVista = MiVista(this)
        setContentView(miVista)

        xMax = tamanio.x.toFloat() - anchoPelota
        yMax = tamanio.y.toFloat() - alturaPorteria

        //posicion de la pelota inicial
        posX = ((xMax + anchoPelota) / 2) - (anchoPelota / 2)
        posY = ((yMax + anchoPelota) / 2) - (anchoPelota / 2)

        //Posiciones de las porterias
        porteriaInicio = ((xMax + anchoPelota) / 2) - (anchoPorteria / 2)
        porteriaFinal = porteriaInicio + anchoPorteria

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorACCELEROMETER = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0?.sensor!!.getType() == Sensor.TYPE_ACCELEROMETER && !esGol) {
            acelerometroX = p0?.values!!.get(0)
            acelerometroY = -p0?.values!!.get(1)
            actualizarPosicion()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onResume() {
        super.onResume()
        if (sensorACCELEROMETER != null) {
            sensorManager.registerListener(
                this,
                sensorACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (sensorACCELEROMETER != null) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun actualizarPosicion() {
        //Calcular nueva posicion
        velocidadX += (acelerometroX * frameTime)
        velocidadY += (acelerometroY * frameTime)

        val xS = (velocidadX / 2 * frameTime)
        val yS = (velocidadY / 2 * frameTime)

        posX -= xS
        posY -= yS

        var anoto = false

        val lPort = 50

        //Verificar limites de la posicion en x
        if (posX > xMax) {
            posX = xMax
        } else if (posX < 0f) {
            posX = 0f
        } else if (posX > porteriaInicio - anchoPelota && posX < porteriaInicio //Lados de la porteria
            && (posY > yMax - alturaPorteria || posY < alturaPorteria)) {
            posX = porteriaInicio - anchoPelota
        } else if (posX < porteriaFinal && posX > porteriaFinal - lPort && (posY > yMax - alturaPorteria || posY < alturaPorteria)) {
            posX = porteriaFinal
            //Verificar si se anoto gol
        } else if (!esGol && posX > porteriaInicio && posX < porteriaFinal && (posY > yMax + alturaPelota - alturaPorteria ||
                    posY < alturaPorteria - alturaPelota)) {
            esGol = true
            if (posY < alturaPorteria - alturaPelota) {
                scoreA++
                anoto = true
            } else if (posY > yMax + alturaPelota - alturaPorteria) {
                scoreB++
                anoto = false
            }
            //Mostrar alerta con indicadores
            val builder = AlertDialog.Builder(this)
            if (anoto) {
                builder.setTitle("¡¡¡Gooool!!!")
                    .setMessage("Anoto el equipo de las Chivas\nChivas: $scoreA\n\n America: $scoreB")
            } else {
                builder.setTitle("¡¡¡Gooool!!!")
                    .setMessage("Anoto el equipo del America\nChivas: $scoreA\n\n America: $scoreB")
            }
            builder.setPositiveButton("Seguir Jugando") { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                posX = (xMax + anchoPelota) / 2 - anchoPelota / 2
                posY = (yMax + anchoPelota) / 2 - anchoPelota / 2
                esGol = false
            }

            //reinicio de valores y posiciones iniciales
            builder.setNegativeButton("Reiniciar Juego") { dialog: DialogInterface, id: Int ->
                scoreA = 0
                scoreB = 0
                posX = (xMax + anchoPelota) / 2 - anchoPelota / 2
                posY = (yMax + anchoPelota) / 2 - anchoPelota / 2
                esGol = false
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }

        //Verificar limites de la posicion en y
        if (posY > yMax) {
            posY = yMax
        } else if (posY < 0) {
            posY = 0f
        }
    }
    private class MiVista(context: Context?) : View(context) {
        private val metrics: DisplayMetrics = DisplayMetrics()
        private val height: Int
        private val width: Int

        init {
            (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(metrics)
            height = metrics.heightPixels
            width = metrics.widthPixels

            //Creacion de la pelota
            val pelotaSrc = BitmapFactory.decodeResource(resources, R.drawable.pelota)
            pelota = Bitmap.createScaledBitmap(pelotaSrc, anchoPelota, alturaPelota, true)

            //Creacion de las porterias
            val porteriaSrcS = BitmapFactory.decodeResource(resources, R.drawable.porteria_up)
            porteriaA = Bitmap.createScaledBitmap(porteriaSrcS, anchoPorteria, alturaPorteria, true)
            val porteriaSrcI = BitmapFactory.decodeResource(resources, R.drawable.porteria)
            porteriaB = Bitmap.createScaledBitmap(porteriaSrcI, anchoPorteria, alturaPorteria, true)

            //Creacion del fondo
            val canchaSrc = BitmapFactory.decodeResource(resources, R.drawable.cancha)
            cancha = Bitmap.createScaledBitmap(canchaSrc, width, height - 50, true)
            Log.d("Tamaños", "$height $width")
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawBitmap(cancha, 0f, -50f, null)


            //Dibujar porterias
            canvas.drawBitmap(porteriaA, porteriaInicio, -50f, null)
            canvas.drawBitmap(porteriaB, porteriaInicio, yMax - 30 + alturaPelota - 200, null)

            //Dibujar pelota
            canvas.drawBitmap(pelota, posX, posY, null)

            //Dibujar cancha
            canvas.drawBitmap(pelota, posX, posY, null)
            invalidate()
        }
    }

}