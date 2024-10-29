package dev.realtards.kuenyawz.services;

import dev.realtards.kuenyawz.dtos.product.VariantDto;
import dev.realtards.kuenyawz.dtos.product.VariantPatchDto;
import dev.realtards.kuenyawz.dtos.product.VariantPostDto;
import dev.realtards.kuenyawz.entities.Product;
import dev.realtards.kuenyawz.entities.Variant;
import dev.realtards.kuenyawz.exceptions.IllegalOperationException;
import dev.realtards.kuenyawz.exceptions.ResourceNotFoundException;
import dev.realtards.kuenyawz.mapper.VariantMapper;
import dev.realtards.kuenyawz.repositories.ProductRepository;
import dev.realtards.kuenyawz.repositories.VariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class VariantServiceImpl implements VariantService {

	private final ProductRepository productRepository;
	private final VariantRepository variantRepository;
	private final VariantMapper variantMapper;

	/**
	 * Master method to get all variants.
	 *
	 * @return {@link List} of {@link VariantDto}
	 */
	@Override
	public List<VariantDto> getAllVariants() {
		List<Variant> variants = variantRepository.findAll();

		List<VariantDto> variantDtos = variants.stream().map(variantMapper::fromEntity).toList();
		return variantDtos;
	}

	/**
	 * Creates a variant and connect it to an existing product.
	 *
	 * @param productId      {@link Long}
	 * @param variantPostDto {@link VariantPostDto}
	 * @return {@link Variant}
	 * @throws ResourceNotFoundException if the product is not found
	 */
	@Override
	public VariantDto createVariant(Long productId, VariantPostDto variantPostDto) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found"));

		Variant variant = Variant.builder()
			.price(variantPostDto.getPrice())
			.type(variantPostDto.getType())
			.product(product)
			.build();
		product.getVariants().add(variant);

		Variant savedVariant = variantRepository.save(variant);

		// Convert and return
		VariantDto variantDto = variantMapper.fromEntity(savedVariant);
		return variantDto;
	}

	/**
	 * Creates multiple variants and connect it to an existing product.
	 *
	 * @param productId       {@link Long}
	 * @param variantPostDtos {@link Iterable} of {@link VariantPostDto}
	 * @return {@link List} of {@link VariantDto}
	 * @throws ResourceNotFoundException if the product is not found
	 */
	@Override
	public List<VariantDto> createVariants(Long productId, VariantPostDto... variantPostDtos) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found"));

		Set<Variant> variants = new HashSet<>();
		for (VariantPostDto dto : variantPostDtos) {
			Variant variant = Variant.builder()
				.price(dto.getPrice())
				.type(dto.getType())
				.product(product)
				.build();
			variants.add(variant);
		}
		product.getVariants().addAll(variants);

		List<Variant> savedVariants = variantRepository.saveAll(variants);
		log.info("CREATED MULTIPLE: {}", savedVariants);

		// Convert and return
		List<VariantDto> variantDtos = savedVariants.stream().map(variantMapper::fromEntity).toList();
		return variantDtos;
	}

	/**
	 * Gets a variant by its ID.
	 *
	 * @param variantId {@link Long}
	 * @return {@link VariantDto}
	 */
	@Override
	public VariantDto getVariant(long variantId) {
		Variant variant = variantRepository.findById(variantId)
			.orElseThrow(() -> new ResourceNotFoundException("Variant with ID '" + variantId + "' not found"));

		// Convert and return
		VariantDto variantDto = variantMapper.fromEntity(variant);
		return variantDto;
	}

	@Override
	public List<VariantDto> getVariantsOfProductId(Long productId) {
		List<Variant> variants = variantRepository.findAllByProduct_ProductId(productId);
		List<VariantDto> variantDtos = variants.stream().map(variantMapper::fromEntity).toList();
		return variantDtos;
	}

	/**
	 * Patches the variant using Mapper.
	 *
	 * @param productId {@link Long}
	 * @param variantId {@link Long}
	 * @param variantPatchDto {@link VariantPatchDto}
	 * @return {@link VariantDto}
	 * @throws ResourceNotFoundException if the variant is not found
	 */
	@Override
	public VariantDto patchVariant(Long productId, Long variantId, VariantPatchDto variantPatchDto) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product with ID '" + productId + "' not found");
        }

		Variant variant = variantRepository.findById(variantId)
			.orElseThrow(() -> new ResourceNotFoundException("Variant with ID '" + variantId + "' not found"));

		Variant updatedVariant = variantMapper.updateVariantFromPatch(variantPatchDto, variant);
		Variant savedVariant = variantRepository.save(updatedVariant);
		log.info("UPDATED: {}", savedVariant);

		// Convert and return
		VariantDto variantDto = variantMapper.fromEntity(savedVariant);
		return variantDto;
	}

	/**
	 * Deletes a variant from a product. Checks the variant, then deletes it.
	 *
	 * @param productId {@link Long}
	 * @param variantId {@link Long}
	 * @throws IllegalOperationException if the product only has one variant
	 * @throws ResourceNotFoundException if the variant is not found
	 */
	@Override
	public void deleteVariant(Long productId, Long variantId) {
		long variantCount = variantRepository.countVariantsByProduct_ProductId(productId);
		if (variantCount <= 1) {
			throw new IllegalOperationException("Cannot delete the last variant of a product");
		}

		int deleted = variantRepository.deleteByVariantIdAndProduct_ProductId(variantId, productId);
		if (deleted == 0) {
			throw new ResourceNotFoundException("Variant with ID '" + variantId + "' not found in Product with ID '" + productId + "'");
		}
		log.info("DELETED: {}", variantId);
	}
}
