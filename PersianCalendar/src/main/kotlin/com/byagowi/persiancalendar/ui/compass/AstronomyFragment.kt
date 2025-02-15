package com.byagowi.persiancalendar.ui.compass

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.databinding.FragmentAstronomyBinding
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.entities.Season
import com.byagowi.persiancalendar.global.coordinates
import com.byagowi.persiancalendar.ui.calendar.dialogs.showDayPickerDialog
import com.byagowi.persiancalendar.ui.shared.ArrowView
import com.byagowi.persiancalendar.ui.utils.getCompatDrawable
import com.byagowi.persiancalendar.ui.utils.onClick
import com.byagowi.persiancalendar.ui.utils.setupMenuNavigation
import com.byagowi.persiancalendar.utils.Eclipse
import com.byagowi.persiancalendar.utils.formatDateAndTime
import com.byagowi.persiancalendar.utils.isRtl
import com.byagowi.persiancalendar.utils.toCivilDate
import com.byagowi.persiancalendar.utils.toJavaCalendar
import com.cepmuvakkit.times.posAlgo.SunMoonPosition
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.persiancalendar.calendar.CivilDate
import io.github.persiancalendar.calendar.PersianDate
import java.util.*

class AstronomyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAstronomyBinding.inflate(layoutInflater)
        binding.appBar.toolbar.let {
            it.setTitle(R.string.astronomical_info)
            it.setupMenuNavigation()
        }

        val resetButton = binding.appBar.toolbar.menu.add(R.string.return_to_today).also {
            it.icon =
                binding.appBar.toolbar.context.getCompatDrawable(R.drawable.ic_restore_modified)
            it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            it.isVisible = false
        }

        val args by navArgs<AstronomyFragmentArgs>()
        val minutesInDay = 60 * 24
        var offset = args.dayOffset * minutesInDay

        val tropicalSwitch = SwitchMaterial(binding.appBar.toolbar.context)
        tropicalSwitch.setText(R.string.tropical)

        fun updateSolarView(time: GregorianCalendar, it: SunMoonPosition) {
            val tropical = tropicalSwitch.isChecked
            val sunZodiac =
                if (tropical) it.sunEcliptic.tropicalZodiac else it.sunEcliptic.iauZodiac
            val moonZodiac =
                if (tropical) it.moonEcliptic.tropicalZodiac else it.moonEcliptic.iauZodiac
            binding.sunText.text = sunZodiac.format(binding.root.context, true) // ☉☀️
            binding.moonText.text =
                moonZodiac.format(binding.root.context, true) // ☽it.moonPhaseEmoji
            binding.time.text = time.formatDateAndTime()
        }

        tropicalSwitch.setOnClickListener {
            val time = GregorianCalendar().also { it.add(Calendar.MINUTE, offset) }
            binding.solarView.setTime(time, true) { updateSolarView(time, it) }
            binding.solarView.isTropicalDegree = tropicalSwitch.isChecked
        }

        binding.appBar.toolbar.menu.add(R.string.goto_date).also { menuItem ->
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menuItem.actionView = tropicalSwitch
        }

        listOf(
            binding.firstSeasonChip to 4, binding.secondSeasonChip to 7,
            binding.thirdSeasonChip to 10, binding.fourthSeasonChip to 1
        ).map { (chip, month) -> // 'month' is month number of first Persian month in the season
            val season = Season.fromPersianCalendar(PersianDate(1400, month, 1), coordinates)
            chip.setText(season.nameStringId)
            chip.chipBackgroundColor = ColorStateList.valueOf(season.color)
        }

        fun update(immediate: Boolean) {
            resetButton.isVisible = offset != 0
            val time = GregorianCalendar().also { it.add(Calendar.MINUTE, offset) }
            binding.solarView.setTime(time, immediate) { updateSolarView(time, it) }

            val persianYear = PersianDate(time.toCivilDate()).year
            binding.headerInformation.text = listOf(
                R.string.solar_eclipse to Eclipse.Category.SOLAR,
                R.string.lunar_eclipse to Eclipse.Category.LUNAR
            ).joinToString("\n") { (title, eclipseCategory) ->
                val eclipse = Eclipse(time, eclipseCategory, true)
                val date = eclipse.maxPhaseDate.toJavaCalendar().formatDateAndTime()
                val type = eclipse.type.name
                    .replace(Regex("^(Solar(Central)?|Lunar)"), "")
                    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                getString(R.string.eclipse_of_type_in).format(getString(title), type, date)
            }

            (1..4).forEach {
                val date = CivilDate(PersianDate(persianYear, it * 3, 29))
                when (it) {
                    1 -> binding.firstSeasonText
                    2 -> binding.secondSeasonText
                    3 -> binding.thirdSeasonText
                    else -> binding.fourthSeasonText
                }.text = Season.values()[it % 4].getEquinox(date).formatDateAndTime()
            }
        }
        update(false)

        binding.appBar.toolbar.menu.add(R.string.goto_date).also {
            it.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            it.onClick {
                val startJdn =
                    Jdn(GregorianCalendar().also { it.add(Calendar.MINUTE, offset) }.toCivilDate())
                showDayPickerDialog(activity ?: return@onClick, startJdn, R.string.go) { jdn ->
                    offset = (jdn - Jdn.today()) * minutesInDay
                    update(false)
                }
            }
        }

        resetButton.onClick {
            offset = 0
            resetButton.isVisible = false
            update(false)
        }

        val viewDirection = if (resources.isRtl) -1 else 1

        var lastButtonClickTimestamp = System.currentTimeMillis()
        binding.slider.smoothScrollBy(200 * viewDirection, 0)

        binding.slider.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (System.currentTimeMillis() - lastButtonClickTimestamp < 2000) return
                offset += dx * viewDirection
                update(true)
            }
        })

        fun buttonScrollSlider(days: Int): Boolean {
            lastButtonClickTimestamp = System.currentTimeMillis()
            binding.slider.smoothScrollBy(50 * days * viewDirection, 0)
            offset += days * minutesInDay
            update(false)
            return true
        }
        binding.startArrow.rotateTo(ArrowView.Direction.START)
        binding.startArrow.setOnClickListener { buttonScrollSlider(-1) }
        binding.startArrow.setOnLongClickListener { buttonScrollSlider(-365) }
        binding.startArrow.contentDescription =
            getString(R.string.previous_x, getString(R.string.day))
        binding.endArrow.rotateTo(ArrowView.Direction.END)
        binding.endArrow.setOnClickListener { buttonScrollSlider(1) }
        binding.endArrow.setOnLongClickListener { buttonScrollSlider(365) }
        binding.endArrow.contentDescription = getString(R.string.next_x, getString(R.string.day))

        return binding.root
    }
}
