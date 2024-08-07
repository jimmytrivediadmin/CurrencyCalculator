package jimmytrivedi.`in`.currencyconvertor.app.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import jimmytrivedi.`in`.currencyconvertor.R
import jimmytrivedi.`in`.currencyconvertor.app.base.BaseActivity
import jimmytrivedi.`in`.currencyconvertor.app.history.ConversionHistoryFragment
import jimmytrivedi.`in`.currencyconvertor.databinding.ActivityCurrencyBinding
import jimmytrivedi.`in`.currencyconvertor.domain.local.data.ConversionHistoryEntity
import jimmytrivedi.`in`.currencyconvertor.domain.remote.data.exchangerate.ExchangeRate
import jimmytrivedi.`in`.currencyconvertor.global.utility.AppConstant
import jimmytrivedi.`in`.currencyconvertor.global.utility.AppUtils
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CurrencyActivity : BaseActivity() {
    private lateinit var binding: ActivityCurrencyBinding
    private val viewModel: CurrencyActivityViewModel by viewModels()
    private var getBaseCurrencyPos: Int = -1

    override fun init() {
        binding = ActivityCurrencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    override fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingStatus.collect {
                    if (it.state) {
                        binding.progressBar.show()
                    } else {
                        binding.progressBar.hide()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exchangeRateData.collect {
                    it.data?.data?.result?.let {
                        if (it.isNotEmpty()) {
                            binding.textViewResult.text = getString(R.string.result_will_be_displayed_here_with_colon, it)

                            // Storing offline
                            val entity = ConversionHistoryEntity(
                                baseCurrency = binding.baseCurrencyLayout.editText?.text.toString(),
                                targetCurrency = binding.targetCurrencyLayout.editText?.text.toString(),
                                amount = binding.amount.editText?.text.toString(),
                                result = it
                            )
                            viewModel.insertConversionHistoryData(entity)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.baseCurrencyData.collect {
                    if (it >= 0) {
                        // Load saved default currency if exists
                        getBaseCurrencyPos = it
                        binding.baseSpinner.setText(binding.baseSpinner.adapter.getItem(it).toString(), false)
                        binding.defaultCurrency.isChecked = true
                    }
                }
            }
        }
    }

    override fun getBundleParameters(bundle: Bundle) {}

    private fun initViews() {
        binding.progressBar.hide()
        setToolbar()
        setSpinner()
        setListener()
        viewModel.getBaseCurrencyData()
    }

    private fun setFragment() {
        supportFragmentManager.fragments.forEach { _ -> supportFragmentManager.popBackStack() }
        val fragment = ConversionHistoryFragment.newInstance(null)
        fragmentTransaction(binding.fragmentContainer.id, fragment, true, AppConstant.Animation.RIGHT_TO_LEFT)
    }

    private fun setToolbar() {
        binding.toolbar.let {
            setSupportActionBar(it)
            it.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.color_white))
        }
    }

    private fun setListener() {
        // Amount listener
        binding.amount.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!AppUtils.checkValidation(binding.baseCurrencyLayout.editText?.text)) {
                    return
                }

                if (!AppUtils.checkValidation(binding.targetCurrencyLayout.editText?.text)) {
                    return
                }

                // If only one char is there and when we press back from amount box, API call should not happen
                if (!AppUtils.checkValidation(binding.amount.editText?.text)) {
                    return
                }

                val exchangeRate = ExchangeRate()
                exchangeRate.totalAmount = s.toString()
                exchangeRate.baseCurrency = binding.baseCurrencyLayout.editText?.text.toString()
                exchangeRate.targetCurrency = binding.targetCurrencyLayout.editText?.text.toString()

                viewModel.getExchangeRateData(exchangeRate)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Spinner base currency listener
        binding.baseCurrencyLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Uncheck base currency
                if (getSelectedPosition() != getBaseCurrencyPos) {
                    binding.defaultCurrency.isChecked = false
                } else {
                    binding.defaultCurrency.isChecked = true
                }

                if (!AppUtils.checkValidation(binding.amount.editText?.text)) {
                    return
                }

                if (!AppUtils.checkValidation(binding.targetCurrencyLayout.editText?.text)) {
                    return
                }

                val exchangeRate = ExchangeRate()
                exchangeRate.totalAmount = binding.amount.editText?.text.toString()
                exchangeRate.baseCurrency = s.toString()
                exchangeRate.targetCurrency = binding.targetCurrencyLayout.editText?.text.toString()

                viewModel.getExchangeRateData(exchangeRate)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Spinner target currency listener
        binding.targetCurrencyLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!AppUtils.checkValidation(binding.amount.editText?.text)) {
                    return
                }

                if (!AppUtils.checkValidation(binding.baseCurrencyLayout.editText?.text)) {
                    return
                }

                val exchangeRate = ExchangeRate()
                exchangeRate.totalAmount = binding.amount.editText?.text.toString()
                exchangeRate.baseCurrency = binding.baseCurrencyLayout.editText?.text.toString()
                exchangeRate.targetCurrency = s.toString()

                viewModel.getExchangeRateData(exchangeRate)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Swap button listener
        binding.swapButton.setOnClickListener {
            val baseCurrency = binding.baseCurrencyLayout.editText?.text.toString()
            val targetCurrency = binding.targetCurrencyLayout.editText?.text.toString()
            binding.baseCurrencyLayout.editText?.setText(targetCurrency)
            binding.targetCurrencyLayout.editText?.setText(baseCurrency)
        }

        // Default base currency check listener
        binding.defaultCurrency.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                viewModel.setBaseCurrencyData(getSelectedPosition())
            }
        }
    }

    private fun setSpinner() {
        val currencies = resources.getStringArray(R.array.currency_array)
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, currencies)

        binding.baseSpinner.setAdapter(adapter)
        binding.targetSpinner.setAdapter(adapter)

    }

    // Function to get the position of the selected item
    private fun getSelectedPosition(): Int {
        val currencies = resources.getStringArray(R.array.currency_array)
        val selectedItem = binding.baseSpinner.text.toString()
        return currencies.indexOf(selectedItem)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_currency, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_history) {
            setFragment()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
