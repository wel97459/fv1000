

test_ctl_port     = 80h
print_port        = 81h
int_timeout_port  = 90h
	jp	main

int_entry:
	exx

	ld	b, a
	ld	hl, int_seen_str

print_str:
	ld	a, (hl)
	cp	0
	jp	z, print_str_exit
	out	(print_port), a
	inc	hl
	jp	print_str

print_str_exit:
	ld	a, b
	exx

	ld	h, 1
	reti

main:
	ld	h,  0
	ld	bc, 100
	ld	a, 50
	out	(int_timeout_port), a

test_timeout_loop:
	ld	a, 1
	cp	h
	jp	z, test_pass

	dec	bc
	jp	nz, test_timeout_loop

test_fail:
	ld	a, 2
	out	(test_ctl_port), a
	db	76h		; hlt

test_pass:
	ld	a, 1
	out (test_ctl_port), a
	db	76h		; hlt

int_seen_str:
	db "Interrupt asserted"
	db 0ah
	db 00h
