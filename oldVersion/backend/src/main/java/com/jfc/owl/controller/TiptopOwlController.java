package com.jfc.owl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import com.jfc.owl.service.BomOwlExportService;
import com.jfc.owl.service.BomOwlExportService.BomTreeNode;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.BmaFile;
import com.jfc.rdb.tiptop.entity.BmbFile;
import com.jfc.rdb.tiptop.entity.ImaFile;
import com.jfc.rdb.tiptop.model.dto.ImaDTO;
import com.jfc.rdb.tiptop.repository.BmaRepository;
import com.jfc.rdb.tiptop.repository.BmbRepository;
import com.jfc.rdb.tiptop.repository.ImaRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

@RestController
@RequestMapping("/bom")
public class TiptopOwlController extends AbstractDTOController<ImaFile> {
	@Autowired
	private BomOwlExportService bomOwlExportService;

	@Autowired
	private ImaRepository imaRepository;

	@Autowired
	private BmaRepository bmaRepository;

	@Autowired
	private BmbRepository bmbRepository;

	@Value("${tiptop.owl.export.path}")
	private String exportPath;

	/**
	 * Get all materials
	 */
	@GetMapping("/materials")
	public ResponseEntity<ApiResponse<List<ImaFile>>> getAllMaterials() {
		try {
			List<ImaFile> materials = imaRepository.findByIma09AndIma10("S", "130 HC");
			return success(materials);
		} catch (Exception e) {
			return error("Failed to retrieve materials: " + e.getMessage());
		}
	}

	/**
	 * Search materials by name
	 */
	@GetMapping("/materials/search")
	public ResponseEntity<ApiResponse<List<ImaFile>>> searchMaterials(@RequestParam String query) {
		try {
			// Search by either code, name or specification
			List<ImaFile> byName = imaRepository.findByIma02ContainingIgnoreCase(query);
			List<ImaFile> bySpec = imaRepository.findByIma021ContainingIgnoreCase(query);

			// Combine results, removing duplicates
			Set<ImaFile> combinedResults = new HashSet<>(byName);
			combinedResults.addAll(bySpec);

			return success(new ArrayList<>(combinedResults));
		} catch (Exception e) {
			return error("Error searching materials: " + e.getMessage());
		}
	}

	/**
	 * Get material by Type & ProductLine
	 */
	@GetMapping("/materials/product")
	public ResponseEntity<ApiResponse<List<ImaFile>>> searchByProductTypeAndLine(
			@RequestParam(required = false) String type, @RequestParam(required = false) String line) {

		try {
			// Call the repository method to search by ima09 (type) and ima10 (line)
			List<ImaFile> results = imaRepository.findByIma09AndIma10(type, line);

			if (results.isEmpty()) {
				return success(results); // Return empty list with success status
			}

			return success(results);

		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Get material by code
	 */
	@GetMapping("/materials/{code}")
	public ResponseEntity<ApiResponse<ImaDTO>> getMaterialByCode(@PathVariable String code) {
		try {
			System.out.println("Searching for material with code: " + code);
			Optional<ImaFile> materialOptional = imaRepository.findById(code);
	        
	        if (materialOptional.isPresent()) {
	            ImaFile material = materialOptional.get();
	            // Convert entity to DTO
	            ImaDTO dto = new ImaDTO();
	            dto.setIma01(material.getIma01());
	            dto.setIma02(material.getIma02());
	            dto.setIma021(material.getIma021());
	            // Add other fields as needed, but don't include the ecmFiles collection
	            
	            return success(dto);
	        } else {
	            System.out.println("Material not found with code: " + code);
	            return error("Material not found with code: " + code);
	        }
			
		} catch (Exception e) {
			System.err.println("Error retrieving material: " + e.getMessage());
	        e.printStackTrace();
	        return error("Error retrieving material: " + e.getMessage());
		}
	}

	/**
	 * Get BOM hierarchy for a master item as a tree structure
	 */
	@GetMapping("/tree/{masterItemCode}")
	public ResponseEntity<ApiResponse<BomTreeNode>> getBomTree(@PathVariable String masterItemCode) {
		try {
			BomTreeNode tree = bomOwlExportService.buildBomTree(masterItemCode);
			return success(tree);
		} catch (IllegalArgumentException e) {
			return error("Master item not found: " + e.getMessage());
		} catch (Exception e) {
			return error("Error generating BOM tree: " + e.getMessage());
		}
	}

	/**
	 * Export all BOMs as OWL
	 */
	@GetMapping("/export-all")
	public ResponseEntity<?> exportAllBoms(@RequestParam(defaultValue = "RDF/XML") String format) {

		try {
			String rdfFormat;
			String contentType;
			String fileExtension;

			// Map format parameter to Jena format
			switch (format.toUpperCase()) {
			case "TURTLE":
			case "TTL":
				rdfFormat = "TURTLE";
				contentType = "text/turtle";
				fileExtension = "ttl";
				break;
			case "JSONLD":
			case "JSON-LD":
				rdfFormat = "JSON-LD";
				contentType = "application/ld+json";
				fileExtension = "jsonld";
				break;
			case "N-TRIPLES":
			case "NT":
				rdfFormat = "N-TRIPLES";
				contentType = "application/n-triples";
				fileExtension = "nt";
				break;
			default:
				rdfFormat = "RDF/XML";
				contentType = "application/rdf+xml";
				fileExtension = "owl";
			}

			// Generate the OWL file
			String outputFile = bomOwlExportService.exportAllBomsToOwl(exportPath, rdfFormat);

			// Create a resource from the file
			Path path = Paths.get(outputFile);
			Resource resource = new UrlResource(path.toUri());

			// Return the file as a downloadable resource
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"complete_bom." + fileExtension + "\"")
					.body(resource);

		} catch (Exception e) {
			return error("Error exporting all BOMs: " + e.getMessage());
		}
	}

	/**
	 * Get components for a master item
	 */
	@GetMapping("/components/{masterItemCode}")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComponentsForMasterItem(
			@PathVariable String masterItemCode) {

		try {
			// Find BmaFile entries for this master item
			List<BmaFile> bomHeaders = bmaRepository.findByIdBma01(masterItemCode);

			if (bomHeaders.isEmpty()) {
				return success(Collections.emptyList());
			}

			List<Map<String, Object>> components = new ArrayList<>();

			// Process each BOM header
			for (BmaFile bma : bomHeaders) {
				String characteristicCode = bma.getId().getBma06();

				// Find components for this BOM
				List<BmbFile> bmbList = bmbRepository.findByBmaFile(bma);

				for (BmbFile bmb : bmbList) {
					ImaFile componentIma = imaRepository.findById(bmb.getId().getBmb03()).orElse(null);

					if (componentIma != null) {
						Map<String, Object> component = new HashMap<>();
						component.put("masterItemCode", masterItemCode);
						component.put("componentItemCode", componentIma.getIma01());
						component.put("componentItemName", componentIma.getIma02());
						component.put("componentItemSpec", componentIma.getIma021());
						component.put("sequence", bmb.getId().getBmb02());
						component.put("quantity", bmb.getBmb06());
						component.put("effectiveDate", bmb.getId().getBmb04());
						component.put("expiryDate", bmb.getBmb05());
						component.put("characteristicCode", bmb.getId().getBmb29());
						component.put("parentCharacteristicCode", characteristicCode);

						components.add(component);
					}
				}
			}

			return success(components);

		} catch (Exception e) {
			return error("Error retrieving components: " + e.getMessage());
		}
	}

	/**
	 * Get statistics about the BOM data
	 */
	@GetMapping("/stats")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getBomStats() {
		try {
			long startTime = System.currentTimeMillis();
			Map<String, Object> stats = new HashMap<>();

			// Log start of operation
	        System.out.println("Starting BOM stats query...");
	        
			// Count total items
	        long itemsStartTime = System.currentTimeMillis();
			long totalItems = imaRepository.countByIma09AndIma10("S", "130 HC");
			System.out.println("Total items query took: " + (System.currentTimeMillis() - itemsStartTime) + "ms");
	        stats.put("totalItems", totalItems);

			// Count master items (items that have a BOM)
	        long masterStartTime = System.currentTimeMillis();
			long masterItemsCount = bmaRepository.countByIma09AndIma10("S", "130 HC");
			System.out.println("Master items count query took: " + (System.currentTimeMillis() - masterStartTime) + "ms");
	        stats.put("masterItemsCount", masterItemsCount);

			// Count component items	        
			/*Set<String> componentCodes = new HashSet<>();
			List<BmbFile> allComponents = bmbRepository.findByIma09AndIma10("S", "130 HC");
			for (BmbFile bmb : allComponents) {
				componentCodes.add(bmb.getId().getBmb03());
			}
			stats.put("componentItemsCount", componentCodes.size());*/
	        long componentsStartTime = System.currentTimeMillis();
	        long componentItemsCount = bmbRepository.countDistinctComponentsByMasterItemTypeAndLine("S", "130 HC");
	        System.out.println("Component count query took: " + (System.currentTimeMillis() - componentsStartTime) + "ms");
	        stats.put("componentItemsCount", componentItemsCount);
			

			// Count total BOM relationships
	        long relationshipsStartTime = System.currentTimeMillis();
	        long bomRelationshipsCount = bmbRepository.countBomRelationshipsByMaterItemTypeAndLine("S", "130 HC");
	        System.out.println("Relationships count query took: " + (System.currentTimeMillis() - relationshipsStartTime) + "ms");
	        stats.put("bomRelationshipsCount", bomRelationshipsCount);

	        System.out.println("Total stats operation took: " + (System.currentTimeMillis() - startTime) + "ms");
	        return success(stats);

		} catch (Exception e) {
			return error("Error retrieving BOM statistics: " + e.getMessage());
		}
	}
	
	/**
	 * Get statistics about the BOM data for items with ima09 = 'S' and ima10 = '130 HC'
	 */
	@GetMapping("/stats/filtered")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getBomStatsFiltered(
			@RequestParam(required = false) String type, @RequestParam(required = false) String line) {
	    try {
	        Map<String, Object> stats = new HashMap<>();

	        // Get filtered items where ima09 = 'S' and ima10 = '130 HC'
	        List<ImaFile> filteredItems = imaRepository.findByIma09AndIma10(type, line);
	        stats.put("filteredItemsCount", filteredItems.size());
	        
	        // Extract the codes of filtered items
	        Set<String> filteredItemCodes = new HashSet<>();
	        for (ImaFile item : filteredItems) {
	            filteredItemCodes.add(item.getIma01());
	        }
	        
	        // Count master items (filtered items that have a BOM)
	        long filteredMasterItemsCount = 0;
	        for (String code : filteredItemCodes) {
	            if (!bmaRepository.findByIdBma01(code).isEmpty()) {
	                filteredMasterItemsCount++;
	            }
	        }
	        stats.put("filteredMasterItemsCount", filteredMasterItemsCount);

	        // Count component items and relationships for filtered master items
	        Set<String> filteredComponentCodes = new HashSet<>();
	        List<BmbFile> filteredComponents = new ArrayList<>();
	        
	        for (String masterCode : filteredItemCodes) {
	            List<BmaFile> bomHeaders = bmaRepository.findByIdBma01(masterCode);
	            for (BmaFile bma : bomHeaders) {
	                List<BmbFile> components = bmbRepository.findByBmaFile(bma);
	                filteredComponents.addAll(components);
	                
	                for (BmbFile bmb : components) {
	                    filteredComponentCodes.add(bmb.getId().getBmb03());
	                }
	            }
	        }
	        
	        stats.put("filteredComponentItemsCount", filteredComponentCodes.size());
	        stats.put("filteredBomRelationshipsCount", filteredComponents.size());

	        return success(stats);

	    } catch (Exception e) {
	        return error("Error retrieving filtered BOM statistics: " + e.getMessage());
	    }
	}

	/**
	 * Search for items by complex criteria
	 */
	@PostMapping("/items/search")
	public ResponseEntity<ApiResponse<List<ImaFile>>> searchItemsComplex(
			@RequestBody Map<String, Object> searchCriteria) {
		try {
			String code = (String) searchCriteria.getOrDefault("code", "");
			String name = (String) searchCriteria.getOrDefault("name", "");
			String spec = (String) searchCriteria.getOrDefault("spec", "");

			// Create dynamic query based on provided criteria
			List<ImaFile> results = new ArrayList<>();

			if (!code.isEmpty()) {
				results = imaRepository.findById(code).map(Collections::singletonList).orElse(Collections.emptyList());
			} else if (!name.isEmpty() && !spec.isEmpty()) {
				// Search by both name and spec
				Set<ImaFile> byName = new HashSet<>(imaRepository.findByIma02ContainingIgnoreCase(name));
				Set<ImaFile> bySpec = new HashSet<>(imaRepository.findByIma021ContainingIgnoreCase(spec));

				// Return intersection
				byName.retainAll(bySpec);
				results = new ArrayList<>(byName);
			} else if (!name.isEmpty()) {
				results = imaRepository.findByIma02ContainingIgnoreCase(name);
			} else if (!spec.isEmpty()) {
				results = imaRepository.findByIma021ContainingIgnoreCase(spec);
			}

			return success(results);

		} catch (Exception e) {
			return error("Error searching items: " + e.getMessage());
		}
	}

	/**
	 * Export BOM for a master item as OWL in various formats
	 */
	@GetMapping("/export/{masterItemCode}")
	public ResponseEntity<?> exportBom(@PathVariable String masterItemCode,
			@RequestParam(defaultValue = "RDF/XML") String format) {
		// Create export directory if it doesn't exist
        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            boolean dirCreated = exportDir.mkdirs();
            if (!dirCreated) {
                return error("Failed to create export directory: " + exportPath);
            }
            System.out.println("Created export directory: " + exportPath);
        }

		try {
			String rdfFormat;
			String contentType;
			String fileExtension;

			// Map format parameter to Jena format
			switch (format.toUpperCase()) {
			case "TURTLE":
			case "TTL":
				rdfFormat = "TURTLE";
				contentType = "text/turtle";
				fileExtension = "ttl";
				break;
			case "JSONLD":
			case "JSON-LD":
				rdfFormat = "JSON-LD";
				contentType = "application/ld+json";
				fileExtension = "jsonld";
				break;
			case "N-TRIPLES":
			case "NT":
				rdfFormat = "N-TRIPLES";
				contentType = "application/n-triples";
				fileExtension = "nt";
				break;
			default:
				rdfFormat = "RDF/XML";
				contentType = "application/rdf+xml";
				fileExtension = "owl";
			}

			// Generate the OWL file
			String outputFile = bomOwlExportService.exportBomForMasterItem(masterItemCode, exportPath, rdfFormat);

			// Create a resource from the file
			Path path = Paths.get(outputFile);
			Resource resource = new UrlResource(path.toUri());

			// Return the file as a downloadable resource
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"" + masterItemCode + "_bom." + fileExtension + "\"")
					.body(resource);

		} catch (Exception e) {
			return error("Error exporting BOM: " + e.getMessage());
		}
	}

	/**
	 * Get BOM ontology for a master item as JSON-LD
	 */
	@GetMapping("/ontology/{masterItemCode}")
	public ResponseEntity<?> getBomOntology(@PathVariable String masterItemCode,
			@RequestParam(defaultValue = "JSONLD") String format) {

		try {
			// Get the ontology model
			OntModel model = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);

			// Serialize to the requested format
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			RDFFormat rdfFormat;
			String contentType;

			switch (format.toUpperCase()) {
			case "TURTLE":
			case "TTL":
				rdfFormat = RDFFormat.TURTLE;
				contentType = "text/turtle";
				break;
			case "N-TRIPLES":
			case "NT":
				rdfFormat = RDFFormat.NTRIPLES;
				contentType = "application/n-triples";
				break;
			case "RDF/XML":
			case "XML":
				rdfFormat = RDFFormat.RDFXML;
				contentType = "application/rdf+xml";
				break;
			default:
				rdfFormat = RDFFormat.JSONLD;
				contentType = "application/ld+json";
			}

			RDFDataMgr.write(outputStream, model, rdfFormat);

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(outputStream.toString());

		} catch (IllegalArgumentException e) {
            return error("Master item not found: " + e.getMessage());
        } catch (Exception e) {
            return error("Error generating ontology: " + e.getMessage());
        }
	}
}