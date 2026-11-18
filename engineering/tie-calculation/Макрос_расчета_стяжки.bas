REM ***** LibreOffice Basic *****
Option Explicit

Sub CalculateTieAndConcrete()
    On Error GoTo ErrHandler
    Dim d As Object, inp As Object, st As Object, co As Object, ov As Object
    Dim m As Double, g As Double, a As Double, dia As Double, rs As Double
    Dim nx As Double, nz As Double, txz As Double, rbt As Double
    Dim pN As Double, mom As Double, w As Double, sig As Double, etaS As Double
    Dim avg As Double, rad As Double, s1 As Double, s1mpa As Double, etaC As Double
    Dim steelText As String, concText As String, overall As String
    Dim pi As Double, k As Double

    d = ThisComponent
    inp = d.TextTables.getByName("ИсходныеДанные")
    st = d.TextTables.getByName("РезультатыСтяжка")
    co = d.TextTables.getByName("РезультатыБетон")
    ov = d.TextTables.getByName("ОбщийВывод")

    m = N(inp.getCellByName("B2").String)
    g = N(inp.getCellByName("B3").String)
    a = N(inp.getCellByName("B4").String)
    dia = N(inp.getCellByName("B6").String)
    rs = N(inp.getCellByName("B7").String)
    nx = N(inp.getCellByName("B10").String)
    nz = N(inp.getCellByName("B11").String)
    txz = N(inp.getCellByName("B12").String)
    rbt = N(inp.getCellByName("B13").String)

    pi = 3.14159265358979
    k = 0.00980665

    pN = m * g
    mom = pN * a
    w = pi * dia ^ 3 / 32
    sig = mom / w

    st.getCellByName("B2").String = F(pN)
    st.getCellByName("B3").String = F(mom)
    st.getCellByName("B4").String = F(w)
    st.getCellByName("B5").String = F(sig)
    st.getCellByName("B6").String = F(rs)

    If dia <= 0 Or rs <= 0 Then
        st.getCellByName("B7").String = "—"
        steelText = "НЕТ ДАННЫХ: введите расчётный диаметр d и сопротивление стали Rs."
    Else
        etaS = sig / rs
        st.getCellByName("B7").String = F(etaS)
        If etaS <= 1 Then
            steelText = "Стяжка проходит проверку на изгиб: σ ≤ Rs."
        Else
            steelText = "Стяжка НЕ проходит проверку на изгиб: σ > Rs."
        End If
    End If
    st.getCellByName("B8").String = steelText

    avg = (nx + nz) / 2
    rad = Sqr(((nx - nz) / 2) ^ 2 + txz ^ 2)
    s1 = avg + rad
    s1mpa = s1 * k

    co.getCellByName("B2").String = F(avg)
    co.getCellByName("B3").String = F(rad)
    co.getCellByName("B4").String = F(s1)
    co.getCellByName("B5").String = F(s1mpa)
    co.getCellByName("B6").String = F(rbt)

    If rbt <= 0 Or (nx = 0 And nz = 0 And txz = 0) Then
        co.getCellByName("B7").String = "—"
        concText = "НЕТ ДАННЫХ: введите Nx, Nz и Txz из одной контрольной точки."
    Else
        etaC = 0
        If s1mpa > 0 Then etaC = s1mpa / rbt
        co.getCellByName("B7").String = F(etaC)
        If etaC <= 1 Then
            concText = "Бетон проходит предварительную упругую проверку: σ1 ≤ Rbt."
        Else
            concText = "Бетон НЕ проходит предварительную упругую проверку: σ1 > Rbt."
        End If
    End If
    co.getCellByName("B8").String = concText

    If InStr(steelText, "НЕТ ДАННЫХ") > 0 Or InStr(concText, "НЕТ ДАННЫХ") > 0 Then
        overall = "ОБЩИЙ ВЫВОД НЕ СФОРМИРОВАН: заполните недостающие исходные данные."
    ElseIf InStr(steelText, "НЕ проходит") > 0 Or InStr(concText, "НЕ проходит") > 0 Then
        overall = "ОБЩИЙ ВЫВОД: крепление не проходит одну или обе принятые проверки."
    Else
        overall = "ОБЩИЙ ВЫВОД: обе принятые проверки выполняются в рамках заданной расчётной схемы."
    End If

    ov.getCellByName("A1").String = overall
    MsgBox "Расчёт завершён." & Chr(10) & steelText & Chr(10) & concText & Chr(10) & overall, 64, "Расчёт стяжки"
    Exit Sub

ErrHandler:
    MsgBox "Ошибка макроса: " & Error$ & Chr(10) & "Проверьте таблицы и исходные данные.", 16, "Расчёт"
End Sub

Private Function N(s As String) As Double
    s = Trim(s)
    s = Replace(s, ",", ".")
    N = Val(s)
End Function

Private Function F(x As Double) As String
    F = Replace(Format(x, "0.000"), ".", ",")
End Function
