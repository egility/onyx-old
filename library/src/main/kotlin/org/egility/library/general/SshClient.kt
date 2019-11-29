/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by mbrickman on 07/08/16.
 */


var privateKey =
        """
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAyY5+NcCfLjvwapq/B/MJfn3CoupVFeEw96f4rEnfDrQ3kcvQ
ckfEgyPw88Bv/73pUzSLseQoFcerGwMlG0KQpcrGXRD+ZCBiA8o+2YOSpvRBW5Pp
VIrgmmspxBPOQ9YbEgpJqhFXogSMppPoyJQtIg8+kRJDjflHqcS2INFBzV+vjvG/
OlWMlG3MiE4w2LRTvBi1HCmTQrqS/xdkByNwP4SAGb0ZzoeF83QdhU5tShHOTIaC
Jt2g+hi42aOtgGvpK7dnDkr0VyuVOIITKVkDWlYF5MY0xrwWefQuKdB5elSXMHa2
1mVK87bMySc0fwnlL9AmMQdtO1S+M6QFW3Nx0wIDAQABAoIBAQDIAa86FcefsHj0
wFQ6RjCwpYjj81a1XH4j8zmvdapzw9+0vKQ/EvptC9hJeTdqdwC2wViyEO66FiWz
q15B+77f1iUbKwbVQtEgY6wUWhzHsW+9uTv72cfhU9/hI6o15Jt+Plk0+vqT2qHb
lsCTcX6L4sa6XH5OvwQe8gWs8Rmz+bApwTsaFRmgDwcEoau9TKlTmthXdS4odTY6
hQavek44NY+ofQFbfaf1sXKVQ4WtGOlhRGloi7zfgbd7xsFXLk6p4nDOI8zMhYTo
V7Kb3+FQV+zDwl8l7MNS/6YxZ7SmNhOPtdAiWMTL9rlcPtnLDNphxogkuMZlcjEo
Ixy3PSl5AoGBAPcRz3+YLdP9auipW1/wGErDW0iV3hNA/rU7uqLd2A1WGfryQmvn
NaKo0DcR+1bgfEPQA+rS7ozY8ncyO9fJacR6QVP4WQAgGrRNfrc0n7W43k8th26y
pM/7wkHWcRQ/q6hQwjCHgwCZtUT7taNHqI0Pfbc6AnASDjNAz10wv91nAoGBANDX
in2Ylrj7n+1Y6MLH4VN95OEaDEoxXBLb9NK/WDhubxlIettoOyoxmkxOvxy5XbZQ
cjh8oOQVgMiI7rig7BmJWSeSnk/u9OB4RU2S5Jd/YX/1+eTVONYvxNrU8hZm3A2L
LhJIwVh6Bu0Ud+ijP6R5tSYBaX2DPrr+6B2L+9i1AoGAI/YYsKayzO9O10yHO0CU
GJW0vKzrpPvJ1xE5iikmIFLO90K5vkDqAqH1pH5eifekvq4RU+WdySxupkDOiwsK
9QkiZxl3wtfpayL4dawYLvgYi4fB9a/U41zummyfCuZ1kssmaK/gtn6o7sCAYKK2
esmtb7Tm0+8c2ALv682Dr7MCgYEAieKll4MVkJDh6I1ZMLFTvhJS+aR/FMU+K116
aWLYVnnjdGf8ZbyNw6/4VgVv/QEONH8sysrOV4ky/DQcmV32RG4ApTYSvGfi2gnO
iAdCUhxSPXAkS8fTvObRLEKIi+3hwDdydbP/o/D9fa6T9M9EB84roiowgiQTWNdQ
XaG86DkCgYAYDLeCytyLC0FKx3g3CJZ/63zcWh6iBG9F/VGAe3VeG4W4oQqn2BxM
nAzngY9yUFmll1ZMtPIv6/IiJqsKuM8o45SPVqsH0f5tFGltiXBpM63esrYh5T8N
yJSxDr/PFB4KUVqTcb6MWTDt3Q/DCBjQeNi+oAf3D58KdldIyTMpFg==
-----END RSA PRIVATE KEY-----
"""

var publicKey =
        """
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJjn41wJ8uO/Bqmr8H8wl+fcKi6lUV4TD3p/isSd8OtDeRy9ByR8SDI/DzwG//velTNIux5CgVx6sbAyUbQpClysZdEP5kIGIDyj7Zg5Km9EFbk+lUiuCaaynEE85D1hsSCkmqEVeiBIymk+jIlC0iDz6REkON+UepxLYg0UHNX6+O8b86VYyUbcyITjDYtFO8GLUcKZNCupL/F2QHI3A/hIAZvRnOh4XzdB2FTm1KEc5MhoIm3aD6GLjZo62Aa+krt2cOSvRXK5U4ghMpWQNaVgXkxjTGvBZ59C4p0Hl6VJcwdrbWZUrztszJJzR/CeUv0CYxB207VL4zpAVbc3HT radical
"""

class SshClient {

    companion object {

        fun execute(host: String, command: String): String {
            var sshChannel = JSch()
            sshChannel.addIdentity("radical", privateKey.toByteArray(), publicKey.toByteArray(), null)

            val session = sshChannel.getSession("root", host, 22)
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(60000)

            val channel = session.openChannel("exec") as ChannelExec
            val reader = BufferedReader(InputStreamReader(channel.inputStream))
            channel.setCommand(command)
            channel.connect(60000)

            var result = StringBuilder()
            var msg = reader.readLine()
            while (msg != null) {
                result.appendln(msg)
                msg = reader.readLine()
            }
            channel.disconnect()
            session.disconnect()
            return result.toString()
        }

    }


}

