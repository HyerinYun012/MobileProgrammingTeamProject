package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.petplace.databinding.FragmentSearchResultBinding
import java.io.Serializable

class SearchResultFragment : Fragment() {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // arguments에서 데이터 추출 시 타입 캐스팅 안정성 확보
        val results = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("results", ArrayList::class.java) as? List<RestaurantResponse>
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("results") as? List<RestaurantResponse>
        } ?: emptyList()

        val adapter = SearchResultAdapter(results) { restaurant ->
            val intent = Intent(requireContext(), PlaceInfoActivity::class.java).apply {
                putExtra("restaurant", restaurant as Serializable)
            }
            startActivity(intent)
        }

        binding.rvSearchResult.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSearchResult.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
