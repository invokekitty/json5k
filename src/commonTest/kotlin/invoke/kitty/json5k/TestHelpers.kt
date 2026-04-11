package invoke.kitty.json5k

import invoke.kitty.json5k.parsing.LinePosition
import invoke.kitty.json5k.parsing.SourceReader
import kotlin.test.assertEquals

internal fun SourceReader.checkPosition(line: Int, column: Int? = null) = pos.check(line, column)
internal fun LinePosition.check(line: Int, column: Int? = null) {
    assertEquals(line, this.line.toInt())
    if (column != null) {
        assertEquals(column, this.column.toInt())
    }
}

internal fun InputError.checkPosition(line: Int, column: Int) {
    assertEquals(line, this.line.toInt())
    assertEquals(column, this.column.toInt())
}
