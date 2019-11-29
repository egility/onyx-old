/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.flint

import com.pi4j.wiringpi.Gpio
import com.pi4j.wiringpi.I2C.*
import com.pi4j.wiringpi.Spi
import org.egility.flint.backlight.BLUE
import org.egility.library.general.*
import java.io.File
import java.io.FileReader
import java.lang.Math.round
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

internal val RS_PIN = 25
internal val RESET_PIN = 12
internal val rows = 3
internal val columns = 16

internal object st7036 {

    // table 0
    val COMMAND_CLEAR = 0b00000001
    val COMMAND_HOME = 0b00000010

    val COMMAND_ENTRY = 0b00000100
    val COMMAND_ENTRY_DIRECTION_RIGHT = 0b00000010
    val COMMAND_ENTRY_ENABLE_SHIFT = 0b00000001

    val COMMAND_DISPLAY = 0b00001000
    val COMMAND_DISPLAY_ON = 0b00000100
    val COMMAND_DISPLAY_CURSOR_ON = 0b00000010
    val COMMAND_DISPLAY_BLINK_ON = 0b00000001

    val COMMAND_SCROLL = 0b00010000
    val COMMAND_SCROLL_CURSOR_LEFT = 0b00000000
    val COMMAND_SCROLL_CURSOR_RIGHT = 0b00000100
    val COMMAND_SCROLL_DISPLAY_LEFT = 0b00001000
    val COMMAND_SCROLL_DISPLAY_RIGHT = 0b00001100

    val COMMAND_FUNCTION = 0b00100000
    val COMMAND_FUNCTION_8_BIT = 0b00010000
    val COMMAND_FUNCTION_2_LINE = 0b00001000
    val COMMAND_FUNCTION_DOUBLE_HEIGHT = 0b00000100
    val COMMAND_FUNCTION_TABLE_0 = 0b00000000
    val COMMAND_FUNCTION_TABLE_1 = 0b00000001
    val COMMAND_FUNCTION_TABLE_2 = 0b00000010
    val COMMAND_FUNCTION_TABLE_3 = 0b00000011

    val COMMAND_CURSOR = 0b10000000

    // table 1
    val COMMAND_BIAS = 0b00010100
    val COMMAND_BIAS_1_4 = 0b00001000
    val COMMAND_BIAS_1_5 = 0b00000000

    val COMMAND_EXTRA = 0b01010000
    val COMMAND_EXTRA_DISPLAY_ICON = 0b00001000
    val COMMAND_EXTRA_BOOST = 0b00000100

    val COMMAND_FOLLOWER = 0b01101011

    val COMMAND_CONTRAST = 0b01110000

    val COMMAND_FUNCTION_TABLE_TEMPLATE = COMMAND_FUNCTION or COMMAND_FUNCTION_8_BIT or COMMAND_FUNCTION_2_LINE
    val TABLE_0 = COMMAND_FUNCTION_TABLE_TEMPLATE + COMMAND_FUNCTION_TABLE_0
    val TABLE_1 = COMMAND_FUNCTION_TABLE_TEMPLATE + COMMAND_FUNCTION_TABLE_1
    val TABLE_2 = COMMAND_FUNCTION_TABLE_TEMPLATE + COMMAND_FUNCTION_TABLE_2
    val TABLE_3 = COMMAND_FUNCTION_TABLE_TEMPLATE + COMMAND_FUNCTION_TABLE_3


    val row_offsets = if (rows == 1) arrayOf(0x00) else if (rows == 2) arrayOf(0x00, 0x40) else arrayOf(0x00, 0x10, 0x20)
    var _enabled = true
    var _cursor_enabled = false
    var _cursor_blink = false
    var _double_height = 0


    init {
        Gpio.wiringPiSetupGpio()
        Spi.wiringPiSPISetup(0, 1000000)

        Gpio.pinMode(RS_PIN, Gpio.OUTPUT)
        Gpio.pinMode(RESET_PIN, Gpio.OUTPUT)

        // sort out reset pin
        Gpio.digitalWrite(RESET_PIN, Gpio.LOW)
        Thread.sleep(2)
        Gpio.digitalWrite(RESET_PIN, Gpio.HIGH)
        Thread.sleep(2)

        initializeDisplay()

    }

    fun initializeDisplay() {
        update_display_mode()
        command(COMMAND_ENTRY or COMMAND_ENTRY_DIRECTION_RIGHT)
        set_bias(1)
        set_contrast(50)
        clear()
    }

    fun set_bias(bias: Int = 1) {
        command(COMMAND_BIAS or (bias shl 4) or 1, TABLE_1)
    }

    fun set_contrast(contrast: Int) {
        val value = contrast.fixRange(0, 0x3f)
        val high = (value shr 4) and 0x03
        val low = contrast and 0x0F
        command(COMMAND_EXTRA or COMMAND_EXTRA_BOOST or high, TABLE_1)
        command(COMMAND_FOLLOWER, TABLE_1)
        command(COMMAND_CONTRAST or low, TABLE_1)
    }

    fun set_display_mode(enable: Boolean = true, cursor: Boolean = false, blink: Boolean = false) {
        _enabled = enable
        _cursor_enabled = cursor
        _cursor_blink = blink
        update_display_mode()
    }

    fun clear() {
        command(COMMAND_CLEAR)
        Thread.sleep(2)
    }

    fun write(text: String) {
        Gpio.digitalWrite(RS_PIN, Gpio.HIGH)
        for (char in text) {
            spiWrite(char.toByte())
        }
    }

    fun set_cursor_position(column: Int, row: Int) {
        val columnValue = column.fixRange(0, columns - 1)
        val rowValue = row.fixRange(0, rows - 1)
        val offset = row_offsets[rowValue] + columnValue
        command(COMMAND_CURSOR or offset)
        Thread.sleep(2)
    }

    fun home() {
        command(COMMAND_HOME)
    }

    fun update_display_mode() {
        var mask = COMMAND_DISPLAY or
                (if (_enabled) COMMAND_DISPLAY_ON else 0) or
                (if (_cursor_enabled) COMMAND_DISPLAY_CURSOR_ON else 0) or
                (if (_cursor_blink) COMMAND_DISPLAY_BLINK_ON else 0)

        command(mask)
    }

    fun command(value: Int, table: Int = TABLE_0) {
        Gpio.digitalWrite(RS_PIN, Gpio.LOW)
        spiWrite(table.toByte())
        Thread.sleep(1)
        spiWrite(value.toByte())
        Thread.sleep(1)
    }

    fun spiWrite(byte: Byte) {
        val packet = ByteArray(1)
        packet[0] = byte
        val result = Spi.wiringPiSPIDataRW(0, packet, 1)
    }

}

internal object backlight {

    val I2C_ADDRESS = 0x54
    val CMD_ENABLE_OUTPUT = 0x00
    val CMD_SET_PWM_VALUES = 0x01
    val CMD_ENABLE_LEDS = 0x13
    val CMD_UPDATE = 0x16
    val CMD_RESET = 0x17

    var i2c_addr = I2C_ADDRESS
    var i2c = -1
    var ledCount = 6

    val RED = 2
    val GREEN = 1
    val BLUE = 0

    var isOn = false
    var colour = 0x000000

    fun open() {
        exec("modprobe i2c-dev")
        i2c = wiringPiI2CSetup(i2c_addr)
        if (i2c < 0) {
            debug("sn3218", "unable to open i2c")
        } else {
            debug("sn3218", "i2c OK")
        }
        wiringPiI2CWriteReg8(i2c, CMD_ENABLE_LEDS, 0b00111111)
        wiringPiI2CWriteReg8(i2c, CMD_ENABLE_LEDS + 1, 0b00111111)
        wiringPiI2CWriteReg8(i2c, CMD_ENABLE_LEDS + 2, 0b00111111)
    }

    fun on() {
        wiringPiI2CWriteReg8(i2c, CMD_ENABLE_OUTPUT, 0x01)
        isOn = true
    }

    fun off() {
        wiringPiI2CWriteReg8(i2c, CMD_ENABLE_OUTPUT, 0x00)
        isOn = false
    }

    fun colour(rgb: Int) {
        if ((rgb and 0xffffff) != colour) {
            colour = rgb and 0xffffff
            val blue = rgb and 0xff
            val green = (rgb.shr(8) and 0xff)
            val red = (rgb.shr(16) and 0xff)

            for (led in 0..ledCount - 1) {
                setIntensity(led, RED, red and 0xff)
                setIntensity(led, GREEN, green and 0xff)
                setIntensity(led, BLUE, blue and 0xff)
            }
            update()
        }
    }

    fun setIntensity(led: Int, colour: Int, value: Int) {
        val offset = led.fixRange(0, ledCount - 1) * 3 + colour.fixRange(0, 2)
        //debug("led", "led=$led, colour=$colour, offset=$offset, value=$value")
        wiringPiI2CWriteReg8(i2c, CMD_SET_PWM_VALUES + offset, value and 0xff)
    }

    fun update() {
        wiringPiI2CWriteReg8(i2c, CMD_UPDATE, 0xff)
    }


}


internal object cap1166 {
    // DEVICE MAP
    val DEFAULT_ADDR = 0x28

    // Supported devices
    val PID_CAP1208 = 0b01101011
    val PID_CAP1188 = 0b01010000
    val PID_CAP1166 = 0b01010001

    // REGISTER MAP

    val R_MAIN_CONTROL = 0x00
    val R_GENERAL_STATUS = 0x02
    val R_INPUT_STATUS = 0x03
    val R_LED_STATUS = 0x04
    val R_NOISE_FLAG_STATUS = 0x0A

    // Read-only delta counts for all inputs
    val R_INPUT_1_DELTA = 0x10
    val R_INPUT_2_DELTA = 0x11
    val R_INPUT_3_DELTA = 0x12
    val R_INPUT_4_DELTA = 0x13
    val R_INPUT_5_DELTA = 0x14
    val R_INPUT_6_DELTA = 0x15
    val R_INPUT_7_DELTA = 0x16
    val R_INPUT_8_DELTA = 0x17

    val R_SENSITIVITY = 0x1F
    // B7     = N/A
    // B6..B4 = Sensitivity
    // B3..B0 = Base Shift

    /*
    val SENSITIVITY = { 128: 0b000, 64:0b001, 32:0b010, 16:0b011, 8:0b100, 4:0b100, 2:0b110, 1:0b111 }
    */

    val R_GENERAL_CONFIG = 0x20
    // B7 = Timeout
    // B6 = Wake Config ( 1 = Wake pin asserted )
    // B5 = Disable Digital Noise ( 1 = Noise threshold disabled )
    // B4 = Disable Analog Noise ( 1 = Low frequency analog noise blocking disabled )
    // B3 = Max Duration Recalibration ( 1 =  Enable recalibration if touch is held longer than max duration )
    // B2..B0 = N/A

    val R_INPUT_ENABLE = 0x21


    val R_INPUT_CONFIG = 0x22

    val R_INPUT_CONFIG2 = 0x23 // Default 0x00000111

    // Values for bits 3 to 0 of R_INPUT_CONFIG2
    // Determines minimum amount of time before
    // a "press and hold" event is detected.

    // Also - Values for bits 3 to 0 of R_INPUT_CONFIG
    // Determines rate at which interrupt will repeat
    //
    // Resolution of 35ms, max = 35 + (35 * 0b1111) = 560ms

    val R_SAMPLING_CONFIG = 0x24 // Default 0x00111001
    val R_CALIBRATION = 0x26 // Default 0b00000000
    val R_INTERRUPT_EN = 0x27 // Default 0b11111111
    val R_REPEAT_EN = 0x28 // Default 0b11111111
    val R_MTOUCH_CONFIG = 0x2A // Default 0b11111111
    val R_MTOUCH_PAT_CONF = 0x2B
    val R_MTOUCH_PATTERN = 0x2D
    val R_COUNT_O_LIMIT = 0x2E
    val R_RECALIBRATION = 0x2F

    // R/W Touch detection thresholds for inputs
    val R_INPUT_1_THRESH = 0x30
    val R_INPUT_2_THRESH = 0x31
    val R_INPUT_3_THRESH = 0x32
    val R_INPUT_4_THRESH = 0x33
    val R_INPUT_5_THRESH = 0x34
    val R_INPUT_6_THRESH = 0x35
    val R_INPUT_7_THRESH = 0x36
    val R_INPUT_8_THRESH = 0x37

    // R/W Noise threshold for all inputs
    val R_NOISE_THRESH = 0x38

    // R/W Standby and Config Registers
    val R_STANDBY_CHANNEL = 0x40
    val R_STANDBY_CONFIG = 0x41
    val R_STANDBY_SENS = 0x42
    val R_STANDBY_THRESH = 0x43

    val R_CONFIGURATION2 = 0x44
    // B7 = Linked LED Transition Controls ( 1 = LED trigger is !touch )
    // B6 = Alert Polarity ( 1 = Active Low Open Drain, 0 = Active High Push Pull )
    // B5 = Reduce Power ( 1 = Do not power down between poll )
    // B4 = Link Polarity/Mirror bits ( 0 = Linked, 1 = Unlinked )
    // B3 = Show RF Noise ( 1 = Noise status registers only show RF, 0 = Both RF and EMI shown )
    // B2 = Disable RF Noise ( 1 = Disable RF noise filter )
    // B1..B0 = N/A

    // Read-only reference counts for sensor inputs
    val R_INPUT_1_BCOUNT = 0x50
    val R_INPUT_2_BCOUNT = 0x51
    val R_INPUT_3_BCOUNT = 0x52
    val R_INPUT_4_BCOUNT = 0x53
    val R_INPUT_5_BCOUNT = 0x54
    val R_INPUT_6_BCOUNT = 0x55
    val R_INPUT_7_BCOUNT = 0x56
    val R_INPUT_8_BCOUNT = 0x57
    // LED Controls - For CAP1188 and similar
    val R_LED_OUTPUT_TYPE = 0x71
    val R_LED_LINKING = 0x72
    val R_LED_POLARITY = 0x73
    val R_LED_OUTPUT_CON = 0x74
    val R_LED_LTRANS_CON = 0x77
    val R_LED_MIRROR_CON = 0x79

    // LED Behaviour
    val R_LED_BEHAVIOUR_1 = 0x81 // For LEDs 1-4
    val R_LED_BEHAVIOUR_2 = 0x82 // For LEDs 5-8
    val R_LED_PULSE_1_PER = 0x84
    val R_LED_PULSE_2_PER = 0x85
    val R_LED_BREATHE_PER = 0x86
    val R_LED_CONFIG = 0x88
    val R_LED_PULSE_1_DUT = 0x90
    val R_LED_PULSE_2_DUT = 0x91
    val R_LED_BREATHE_DUT = 0x92
    val R_LED_DIRECT_DUT = 0x93
    val R_LED_DIRECT_RAMP = 0x94
    val R_LED_OFF_DELAY = 0x95

    // R/W Power buttonc ontrol
    val R_POWER_BUTTON = 0x60
    val R_POW_BUTTON_CONF = 0x61

    // Read-only upper 8-bit calibration values for sensors
    val R_INPUT_1_CALIB = 0xB1
    val R_INPUT_2_CALIB = 0xB2
    val R_INPUT_3_CALIB = 0xB3
    val R_INPUT_4_CALIB = 0xB4
    val R_INPUT_5_CALIB = 0xB5
    val R_INPUT_6_CALIB = 0xB6
    val R_INPUT_7_CALIB = 0xB7
    val R_INPUT_8_CALIB = 0xB8

    // Read-only 2 LSBs for each sensor input
    val R_INPUT_CAL_LSB1 = 0xB9
    val R_INPUT_CAL_LSB2 = 0xBA

    // Product ID Registers
    val R_PRODUCT_ID = 0xFD
    val R_MANUFACTURER_ID = 0xFE
    val R_REVISION = 0xFF

    // LED Behaviour settings
    val LED_BEHAVIOUR_DIRECT = 0b00
    val LED_BEHAVIOUR_PULSE1 = 0b01
    val LED_BEHAVIOUR_PULSE2 = 0b10
    val LED_BEHAVIOUR_BREATHE = 0b11

    val LED_OPEN_DRAIN = 0 // Default, LED is open-drain output with ext pullup
    val LED_PUSH_PULL = 1 // LED is driven HIGH/LOW with logic 1/0

    val LED_RAMP_RATE_2000MS = 7
    val LED_RAMP_RATE_1500MS = 6
    val LED_RAMP_RATE_1250MS = 5
    val LED_RAMP_RATE_1000MS = 4
    val LED_RAMP_RATE_750MS = 3
    val LED_RAMP_RATE_500MS = 2
    val LED_RAMP_RATE_250MS = 1
    val LED_RAMP_RATE_0MS = 0

    val number_of_inputs = 6
    val number_of_leds = 6

    var i2c_addr = 0x2c
    var i2c = -1

    var repeat_enabled = 0b00000000
    var release_enabled = 0b11111111

    var _delta = 10

    var handler: (Int) -> Unit = {}

    fun startThread(handler: (Int) -> Unit) {
        this.handler = handler
        thread(name = "cap1166", start = true) {

            open()

            // Enable all inputs with interrupt by default
            enableInputs(0b00111111)
            enableInterrupts(0b00111111)

            // Disable repeat for all channels, but give
            // it sane defaults anyway
            enableRepeat(0b00000000)
            enableMultitouch(true)

            setHoldDelay(210)
            setRepeatRate(210)

            // Tested sane defaults for various configurations
            writeByte(R_SAMPLING_CONFIG, 0b00001000) // 1sample per measure, 1.28ms time, 35ms cycle
            writeByte(R_SENSITIVITY, 0b01100000) // 2x sensitivity
            writeByte(R_GENERAL_CONFIG, 0b00111000)
            writeByte(R_CONFIGURATION2, 0b01100001)

            writeByte(R_CALIBRATION, 0b00111111)

            while (hardware.running) {
                poll()
            }
        }
    }

    fun open() {
        exec("modprobe i2c-dev")
        i2c = wiringPiI2CSetup(i2c_addr)
        if (i2c < 0) {
            debug("cap1166", "unable to open i2c")
        } else {
            debug("cap1166", "i2c OK")
        }
    }

    fun writeByte(register: Int, value: Int, silent: Boolean = false) {
        if (false && !silent) {
            debug("cap1166", "register " + "%02x".format(register) + " <- " + "%02x".format(value))
        }
        wiringPiI2CWriteReg8(i2c, register, value and 0xFF)
    }

    fun readByte(register: Int, silent: Boolean = false): Int {
        val result = wiringPiI2CReadReg8(i2c, register)
        if (false && !silent) {
            debug("cap1166", "register " + "%02x".format(register) + " -> " + "%02x".format(result))
        }
        return result
    }

    fun poll() {
        if (waitForInterrupt()) {
            handleAlert()
        }
    }

    fun handleAlert(pin: Int = -1) {
        /*
        val inputs = get_input_status()
        for (x in 0..number_of_inputs-1) {
            _trigger_handler(x, inputs[x])
        }
        */
        val touched = readByte(R_INPUT_STATUS)
        clearInterrupt()
        for (bit in 0..number_of_inputs - 1)
            if (touched.isBitSet(bit)) {
                val threshold = readByte(R_INPUT_1_THRESH + bit)
                val delta = readByte(R_INPUT_1_DELTA + bit)
                if (twosCompliment(delta) >= threshold) {
                    handler(bit)
                }
            }
    }

    fun twosCompliment(value: Int): Int {
        if ((value and (1 shl (8 - 1))) != 0) {
            return value - (1 shl 8)
        }
        return value
    }

    fun setHoldDelay(ms: Int) {
        // Set time before a press and hold is detected,
        //    Clamps to multiples of 35 from 35 to 560
        val repeat_rate = calcTouchRate(ms)
        var input_config = readByte(R_INPUT_CONFIG2)
        input_config = (input_config and 0b1111.inv()) or repeat_rate
        writeByte(R_INPUT_CONFIG2, input_config)
    }

    fun setRepeatRate(ms: Int) {
        // Set repeat rate in milliseconds,
        //    Clamps to multiples of 35 from 35 to 560
        val repeat_rate = calcTouchRate(ms)
        var input_config = readByte(R_INPUT_CONFIG)
        input_config = (input_config and 0b1111.inv()) or repeat_rate
        writeByte(R_INPUT_CONFIG, input_config)
    }

    fun calcTouchRate(ms: Int): Int {
        val ms2 = ms.fixRange(0, 560)
        return ((round(ms2 / 35.0) * 35).toInt() - 35) / 35
    }


    fun enableMultitouch(enabled: Boolean = true) {
        //Toggles multi-touch by toggling the multi-touch
        //    block bit in the config register
        val ret_mt = readByte(R_MTOUCH_CONFIG)
        if (enabled) {
            writeByte(R_MTOUCH_CONFIG, ret_mt or 0x80)
        } else {
            writeByte(R_MTOUCH_CONFIG, ret_mt and 0x80.inv())
        }
    }

    fun enableRepeat(inputs: Int) {
        repeat_enabled = inputs
        writeByte(R_REPEAT_EN, inputs)
    }

    fun enableInterrupts(inputs: Int) {
        writeByte(R_INTERRUPT_EN, inputs)
    }

    fun enableInputs(inputs: Int) {
        writeByte(R_INPUT_ENABLE, inputs)
    }


    /*
    fun setBit(register: Int, bit: Int) {
        writeByte(register, readByte(register) or (1 shl bit))
    }

    fun clearBit(register: Int, bit: Int) {
        writeByte(register, readByte(register) and (1 shl bit).inv())
    }

    fun changeBit(register: Int, bit: Int, state: Boolean) {
        when (state) {
            true -> setBit(register, bit)
            false -> clearBit(register, bit)
        }
    }

    fun changeBits(register: Int, offset: Int, size: Int, bits: Int) {
        var original_value = readByte(register)
        for (x in 0..size - 1) {
            original_value = original_value and (1 shl offset + x)
        }
        original_value = original_value or (bits shl offset)
        writeByte(register, original_value)
    }
    */

    fun interruptStatus(): Int {
        return readByte(R_MAIN_CONTROL, silent = true) and 1
    }

    fun clearInterrupt() {
        val main = readByte(R_MAIN_CONTROL, silent = true) and 0b00000001.inv()
        writeByte(R_MAIN_CONTROL, main, silent = true)
    }

    fun waitForInterrupt(timeout: Int = 100): Boolean {
        val timeoutTime = cpuTime + timeout
        while (true) {
            val status = interruptStatus()
            if (status != 0) {
                return true
            } else if (cpuTime > timeoutTime) {
                return false
            }
            Thread.sleep(5)
        }
    }

}

internal object lcd {

    fun clear() {
        st7036.clear()
    }

    fun display(message: String) {
        st7036.clear()
        st7036.write(message)
    }

    fun displayAt(message: String, row: Int = 0, column: Int = 0) {
        st7036.set_cursor_position(column, row)
        st7036.write(message)
    }

    fun displayLine(message: String, row: Int = 0) {
        val text = (message + "                ").substring(0, 16)
        displayAt(text, row)
    }

}

internal object touch {
    val I2C_ADDR = 0x2c

    val BACK = 0
    val UP = 1
    val DOWN = 2
    val LEFT = 3
    val SELECT = 4
    val RIGHT = 5

    fun onTouch(handler: (Int) -> Unit) {
        cap1166.startThread(handler)
    }

}

class DisplayotronHat() {

    companion object {
        private val hatName = "Display-o-Tron HAT"

        private var tested = false
        private var _exists = false

        private var menuIndex = -1
        private var menu = ArrayList<String>()
        private var menuTimeout = 0L

        private var backlightTimeout = 0L
        private val backlightOff = 300000L // Switch off after 5 mins

        private val RED = 0x800000
        private val GREEN = 0x008000
        private val BLUE = 0x000080
        private val WHITE = 0x808080

        val exists: Boolean
            get() {
                if (!tested) {
                    val file = File("/proc/device-tree/hat/product")
                    if (file.exists()) {
                        val product = FileReader(file).readText().substring(0, hatName.length)
                        _exists = product == hatName
                    }
                    tested = true
                }
                return _exists
            }

        fun startThread() {
            if (exists) {

                backlight.open()
                backlight.colour(RED)
                backlightTimeout = cpuTime + backlightOff
                backlight.on()
                
                touch.onTouch { button ->
                    press(button)
                }

                thread(name = "led", start = true) {
                    while (hardware.running) {
                        try {
                            if (menuIndex >= 0 && menuTimeout < cpuTime) {
                                menuIndex = -1
                            }
                            if (backlight.isOn && backlightTimeout < cpuTime && !acuStatus.hasError) {
                                backlight.off()
                            }
                            synchronized(lcd) {
                                lcd.displayAt(Date().timeSeconds, 0, 8)
                                lcd.displayAt((acuStatus.errorCodes + "        ").substring(0, 8), 0, 0)
                                lcd.displayLine(acuStatus.mobileLine, 1)
                                if (menuIndex < 0) {
                                    lcd.displayLine(acuStatus.statusLine, 2)
                                }
                                if (acuStatus.hasError) {
                                    if (backlight.colour != RED) {
                                        backlight.colour(RED)
                                        backlightTimeout = cpuTime + backlightOff
                                        backlight.on()
                                    }
                                } else {
                                    if (backlight.colour != GREEN) {
                                        backlight.colour(GREEN)
                                        backlightTimeout = cpuTime + backlightOff
                                        backlight.on()
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            hardware.logError(e)
                        }
                        Thread.sleep(1000)
                    }
                    backlight.off()
                }
            }
        }

        fun displayMenu(index: Int) {
            menuTimeout = cpuTime + 10000
            menuIndex = (index + menu.size).rem(menu.size)
            val text = "${menuIndex + 1} ${menu[menuIndex]}"
            val spacing = (16 - text.length) / 2
            synchronized(lcd, {
                lcd.displayLine("                ".substring(0, spacing) + text, 2)
            })
        }

        fun press(button: Int) {
            when (button) {
                touch.BACK -> {
                    if (menuIndex >= 0) {
                        menuIndex = -1
                        lcd.displayLine(acuStatus.statusLine, 2)
                    }
                }
                touch.UP -> {
                    backlightTimeout = 0L
                    backlight.off()
                }
                touch.DOWN -> {
                    backlightTimeout = cpuTime + backlightOff
                    backlight.on()
                }
                touch.LEFT -> {
                    backlightTimeout = cpuTime + backlightOff
                    backlight.on()
                    if (menuIndex >= 0) {
                        displayMenu(menuIndex - 1)
                    }
                }
                touch.SELECT -> {
                    backlightTimeout = cpuTime + backlightOff
                    backlight.on()
                    if (menuIndex == 0) {
                        menuIndex = -1
                        lcd.displayLine(acuStatus.statusLine, 2)
                    } else if (menuIndex > 0) {
                        val option = menu[menuIndex]
                        menuIndex = -1
                        lcd.displayLine(acuStatus.statusLine, 2)
                        hardware.handleMenu(option)
                    } else {
                        menu.clear()
                        menu.addAll(hardware.menuOptions)
                        displayMenu(0)
                    }
                }
                touch.RIGHT -> {
                    backlightTimeout = cpuTime + backlightOff
                    backlight.on()
                    if (menuIndex >= 0) {
                        displayMenu(menuIndex + 1)
                    }
                }
            }

        }

        fun boot() {
            if (exists) {
                lcd.clear()
                lcd.displayLine("Starting...", 2)
                backlight.open()
                backlight.colour(RED)
                backlightTimeout = cpuTime + backlightOff
                backlight.on()
            }
        }

        fun down() {
            if (exists) {
                lcd.clear()
                lcd.displayLine("Closing Down...", 2)
                backlight.colour(BLUE)
                backlightTimeout = cpuTime + backlightOff
                backlight.on()
            }
        }

        fun term() {
            if (exists) {
                //lcd.clear()
                lcd.displayLine("Disconnect power", 0)
                lcd.displayLine("when green light", 1)
                lcd.displayLine("stops flickering", 2)
                backlight.colour(BLUE)
                backlightTimeout = cpuTime + backlightOff
                backlight.on()
                //backlight.off()
            }
        }


    }


}
