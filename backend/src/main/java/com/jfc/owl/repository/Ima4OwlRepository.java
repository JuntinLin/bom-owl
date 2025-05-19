package com.jfc.owl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jfc.rdb.tiptop.entity.ImaFile;
@Repository
public interface Ima4OwlRepository extends JpaRepository<ImaFile, String> {
	// Find items by name (partial match)
    List<ImaFile> findByIma02ContainingIgnoreCase(String itemName);
    
    // Find items by specification (partial match)
    List<ImaFile> findByIma021ContainingIgnoreCase(String itemSpec);
	/*
	 * // Count total items
			long totalItems = imaRepository.count();
			stats.put("totalItems", totalItems);

			// Count master items (items that have a BOM)
			long masterItemsCount = bmaRepository.count();
			stats.put("masterItemsCount", masterItemsCount);

			// Count component items
			Set<String> componentCodes = new HashSet<>();
			List<BmbFile> allComponents = bmbRepository.findAll();
			for (BmbFile bmb : allComponents) {
				componentCodes.add(bmb.getId().getBmb03());
			}
			stats.put("componentItemsCount", componentCodes.size());
	 * 
	 */
	
	/*2025-04-14 先統計油壓缸的
	 * Count total items
	 * ima09 分群碼1 S N V
	 * ima10 分群碼2 130 HC
	 * */
	
	 // Count items with specific type and line
    @Query("SELECT COUNT(i) FROM ImaFile i WHERE i.ima09 = :type AND i.ima10 = :line")
    long countByIma09AndIma10(String type, String line);
    
    // Find items with specific type and line
    List<ImaFile> findByIma09AndIma10(String type, String line);
}
