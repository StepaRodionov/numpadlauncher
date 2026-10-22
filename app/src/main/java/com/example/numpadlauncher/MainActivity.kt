package com.example.numpadlauncher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var listContainer: LinearLayout
    private lateinit var codeInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("bindings", Context.MODE_PRIVATE)
        listContainer = findViewById(R.id.listContainer)
        codeInput = findViewById(R.id.codeInput)

        findViewById<Button>(R.id.launchButton).setOnClickListener {
            launchByCode(codeInput.text.toString().trim())
        }

        findViewById<Button>(R.id.addButton).setOnClickListener {
            showAddDialog()
        }

        refreshList()
    }

    private fun launchByCode(code: String) {
        if (code.length < 2 || code.length > 6 || !code.all { it.isDigit() }) {
            Toast.makeText(this, "Код должен быть из 2–6 цифр", Toast.LENGTH_SHORT).show()
            return
        }
        val pkg = prefs.getString(code, null)
        if (pkg == null) {
            Toast.makeText(this, "Нет приложения для кода $code", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Приложение не найдено (удалено?)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddDialog() {
        val dialogView = LinearLayout(this)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(40, 20, 40, 20)

        val codeField = EditText(this)
        codeField.hint = "Код (2–6 цифр, любая комбинация)"
        codeField.inputType = InputType.TYPE_CLASS_NUMBER
        dialogView.addView(codeField)

        val apps = getLaunchableApps()
        val appNames = apps.map { it.second }.toTypedArray()
        var selectedIndex = 0

        val appSpinner = Spinner(this)
        appSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appNames)
        appSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedIndex = pos
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        dialogView.addView(appSpinner)

        AlertDialog.Builder(this)
            .setTitle("Новая привязка")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val code = codeField.text.toString().trim()
                if (code.length < 2 || code.length > 6 || !code.all { it.isDigit() }) {
                    Toast.makeText(this, "Код должен быть из 2–6 цифр", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (apps.isEmpty()) return@setPositiveButton
                val pkg = apps[selectedIndex].first
                prefs.edit().putString(code, pkg).apply()
                refreshList()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getLaunchableApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        return resolveInfos
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val all = prefs.all.toSortedMap()
        for ((code, pkgObj) in all) {
            val pkg = pkgObj as? String ?: continue
            val row = layoutInflater.inflate(R.layout.item_binding, listContainer, false)
            val codeText = row.findViewById<TextView>(R.id.codeText)
            val appText = row.findViewById<TextView>(R.id.appText)
            val launchBtn = row.findViewById<Button>(R.id.rowLaunchButton)
            val deleteBtn = row.findViewById<Button>(R.id.rowDeleteButton)

            codeText.text = code
            appText.text = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }

            launchBtn.setOnClickListener { launchByCode(code) }
            deleteBtn.setOnClickListener {
                prefs.edit().remove(code).apply()
                refreshList()
            }

            listContainer.addView(row)
        }
    }
}
