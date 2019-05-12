/*
 *   Copyright (c) 2017-2019 Yui
 *
 *   Permission is hereby granted, free of charge, to any person
 *   obtaining a copy of this software and associated documentation
 *   files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use,
 *   copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following
 *   conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *   OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *   FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *   OTHER DEALINGS IN THE SOFTWARE.
 */

package moe.kyubey.akatsuki.extensions

import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

// https://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle

class UTF8Control : ResourceBundle.Control() {
    override fun newBundle(p0: String, p1: Locale, p2: String, p3: ClassLoader, p4: Boolean): ResourceBundle {
        val bundleName = toBundleName(p0, p1)
        val resourceName = toResourceName(bundleName, "properties")
        var bundle: ResourceBundle? = null
        var stream: InputStream? = null

        if (p4) {
            val url = p3.getResource(resourceName)
            if (url != null) {
                val connection = url.openConnection()
                if (connection != null) {
                    connection.useCaches = false
                    stream = connection.getInputStream()
                }
            }
        } else {
            stream = p3.getResourceAsStream(resourceName)
        }

        if (stream != null) {
            try {
                bundle = PropertyResourceBundle(InputStreamReader(stream, "UTF-8"))
            } finally {
                stream.close()
            }
        }

        return bundle as ResourceBundle
    }
}